package com.defect.platform.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek（OpenAI 兼容协议）配置类
 * <p>全部取值来自 application.yml 的 ai.deepseek.*，禁止硬编码</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekProperties {

    /** API Key，未配置时 AI 能力降级为本地规则引擎 */
    private String apiKey;

    /** 服务地址（OpenAI 兼容） */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名 */
    private String model = "deepseek-chat";

    /** 连接与读取超时（毫秒） */
    private int timeout = 30000;

    /** 采样温度，缺陷判定类任务宜低以保证稳定 */
    private double temperature = 0.3;

    /** 单次回复的最大 token 数 */
    private int maxTokens = 1024;

    /**
     * 是否已完成配置（有 Key 才可发起真实调用）
     */
    public boolean configured() {
        return StrUtil.isNotBlank(apiKey);
    }
}
