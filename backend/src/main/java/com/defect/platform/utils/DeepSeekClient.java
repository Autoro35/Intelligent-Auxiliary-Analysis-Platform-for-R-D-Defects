package com.defect.platform.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.config.DeepSeekProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 客户端统一封装（OpenAI 兼容的 /chat/completions 协议）
 * <p>全平台所有大模型调用必须经由本类，禁止在业务代码中自行发起 HTTP 请求。</p>
 * <p>未配置 api-key 时 {@link #isConfigured()} 返回 false，业务层据此降级为本地规则引擎；
 * 本类自身不做降级，调用失败一律抛出 {@link BusinessException}，由业务层决定是否兜底。</p>
 */
@Slf4j
@Component
public class DeepSeekClient {

    /** 对话补全接口路径（相对 base-url） */
    private static final String CHAT_PATH = "/chat/completions";

    private final DeepSeekProperties properties;

    /** 未配置 api-key 时为 null，避免创建无意义的连接工厂 */
    private final RestClient restClient;

    public DeepSeekClient(DeepSeekProperties properties) {
        this.properties = properties;
        if (properties.configured()) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(properties.getTimeout());
            factory.setReadTimeout(properties.getTimeout());
            this.restClient = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(factory)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            log.info("DeepSeek 客户端已启用: baseUrl={}, model={}", properties.getBaseUrl(), properties.getModel());
        } else {
            this.restClient = null;
            log.warn("DeepSeek 未配置 api-key，AI 能力将降级为本地规则引擎");
        }
    }

    /**
     * 是否已完成配置，可直接发起调用
     */
    public boolean isConfigured() {
        return restClient != null;
    }

    /**
     * 单轮对话，默认最大 token 数
     *
     * @param systemPrompt 系统提示词，可为空
     * @param userPrompt   用户输入
     * @return 模型输出的纯文本内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, properties.getMaxTokens());
    }

    /**
     * 单轮对话
     *
     * @param maxTokens 本次回复的最大 token 数
     */
    public String chat(String systemPrompt, String userPrompt, int maxTokens) {
        if (!isConfigured()) {
            throw new BusinessException(ResultCode.AI_NOT_CONFIGURED);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", properties.getTemperature());
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        List<Map<String, String>> messages = new ArrayList<>(2);
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        long start = System.currentTimeMillis();
        String raw;
        try {
            raw = restClient.post()
                    .uri(CHAT_PATH)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("DeepSeek 调用失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.AI_CALL_FAILED);
        }
        String content = parseContent(raw);
        log.info("DeepSeek 调用完成: 耗时={}ms, 回复长度={}", System.currentTimeMillis() - start,
                content == null ? 0 : content.length());
        return content;
    }

    /**
     * 单轮对话并将回复解析为指定类型（自动剥离 Markdown 代码块围栏）
     *
     * @param type 目标类型，字段名需与模型返回的 JSON 键一致
     */
    public <T> T chatForJson(String systemPrompt, String userPrompt, Class<T> type) {
        String content = chat(systemPrompt, userPrompt);
        String json = extractJson(content);
        if (json == null) {
            log.warn("DeepSeek 回复中未找到 JSON 结构: {}", StrUtil.maxLength(content, 200));
            throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
        }
        try {
            return JSONUtil.toBean(json, type);
        } catch (Exception e) {
            log.warn("DeepSeek 回复 JSON 反序列化失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
        }
    }

    /**
     * 单轮对话并返回 JSON 对象
     */
    public JSONObject chatForJsonObject(String systemPrompt, String userPrompt) {
        String content = chat(systemPrompt, userPrompt);
        String json = extractJson(content);
        if (json == null) {
            log.warn("DeepSeek 回复中未找到 JSON 结构: {}", StrUtil.maxLength(content, 200));
            throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
        }
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.warn("DeepSeek 回复 JSON 解析失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
        }
    }

    /**
     * 连通性探测：发起一次极小的真实调用，会消耗少量 token
     *
     * @return 可用返回 true，未配置或调用失败返回 false
     */
    public boolean ping() {
        if (!isConfigured()) {
            return false;
        }
        try {
            chat("", "ping", 1);
            return true;
        } catch (Exception e) {
            log.warn("DeepSeek 连通性探测失败: {}", e.getMessage());
            return false;
        }
    }

    // ---- 私有 ----

    /**
     * 从 OpenAI 兼容响应体中取出首个 choice 的文本内容
     */
    private String parseContent(String raw) {
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
        }
        try {
            JSONObject root = JSONUtil.parseObj(raw);
            Object choices = root.get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
                log.warn("DeepSeek 响应缺少 choices: {}", StrUtil.maxLength(raw, 200));
                throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
            }
            JSONObject first = JSONUtil.parseObj(((List<?>) choices).get(0));
            JSONObject message = first.getJSONObject("message");
            if (message == null || StrUtil.isBlank(message.getStr("content"))) {
                throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
            }
            return message.getStr("content");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("DeepSeek 响应解析失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.AI_RESPONSE_INVALID);
        }
    }

    /**
     * 从模型回复中提取 JSON 主体：剥离 ```json 围栏，并截取首个 { 到末个 } 之间的内容
     *
     * @return 未找到 JSON 结构时返回 null
     */
    public static String extractJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int fenceEnd = text.lastIndexOf("```");
            if (firstLineEnd > 0 && fenceEnd > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, fenceEnd).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }
}
