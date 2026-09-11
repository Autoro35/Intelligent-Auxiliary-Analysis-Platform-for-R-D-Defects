package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.constant.DefectPriorityEnum;
import com.defect.platform.common.constant.DefectSeverityEnum;
import com.defect.platform.common.constant.DefectTypeEnum;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.config.ChromaProperties;
import com.defect.platform.config.DeepSeekProperties;
import com.defect.platform.dto.AiClassifyDTO;
import com.defect.platform.dto.AiCompleteDTO;
import com.defect.platform.dto.AiRecommendDTO;
import com.defect.platform.entity.Defect;
import com.defect.platform.mapper.DefectMapper;
import com.defect.platform.mapper.KnowledgeMapper;
import com.defect.platform.service.AiService;
import com.defect.platform.service.KnowledgeRetrievalService;
import com.defect.platform.utils.AiRuleEngine;
import com.defect.platform.utils.DeepSeekClient;
import com.defect.platform.vo.AiClassifyVO;
import com.defect.platform.vo.AiCompleteVO;
import com.defect.platform.vo.AiRecommendItemVO;
import com.defect.platform.vo.AiRecommendVO;
import com.defect.platform.vo.AiStatusVO;
import com.defect.platform.vo.AiSuggestionVO;
import com.defect.platform.vo.RetrievalHitVO;
import com.defect.platform.vo.RetrievalResultVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 辅助能力实现
 * <p>统一编排：{@link DeepSeekClient} 负责大模型调用，{@link KnowledgeRetrievalService} 负责 RAG 检索，
 * {@link AiRuleEngine} 负责无大模型时的本地降级。三者互不感知，便于单独替换。</p>
 * <p>降级策略：大模型未配置 / 调用失败 / 返回内容无法解析，一律回退本地规则引擎，接口不向调用方抛错。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    /** RAG 推荐条数，按需求固定为 Top3 */
    private static final int TOP_N = 3;

    /** 送入大模型的描述文本上限，避免 token 浪费 */
    private static final int MAX_PROMPT_DESC_LENGTH = 1000;

    /** 用于检索的文本长度上限，过长会稀释全文索引的相关度 */
    private static final int MAX_QUERY_LENGTH = 300;

    private static final String CLASSIFY_SYSTEM_PROMPT =
            "你是资深研发缺陷分析专家。请阅读缺陷标题与描述，判断缺陷类型、优先级与严重程度。\n"
                    + "可选值（必须严格使用下列英文代码）：\n"
                    + "- type: FUNCTIONAL-功能Bug, PERFORMANCE-性能问题, UI-UI异常, COMPATIBILITY-兼容性问题, OPTIMIZATION-建议优化\n"
                    + "- priority: URGENT-紧急, HIGH-高, MEDIUM-中, LOW-低\n"
                    + "- severity: BLOCKER-致命, CRITICAL-严重, MAJOR-一般, MINOR-轻微\n"
                    + "判定原则：系统崩溃/数据丢失/功能完全不可用取 BLOCKER；核心功能受阻且有明确报错取 CRITICAL；"
                    + "一般功能异常取 MAJOR；样式文案类轻微问题取 MINOR。优先级由严重程度与业务影响共同决定，"
                    + "涉及线上环境、客户投诉可上调。\n"
                    + "只输出一个 JSON 对象，不要输出任何解释文字，也不要使用 Markdown 代码块：\n"
                    + "{\"type\":\"\",\"priority\":\"\",\"severity\":\"\",\"confidence\":0.0,\"reason\":\"判定依据，60字以内\"}";

    private static final String RECOMMEND_SYSTEM_PROMPT =
            "你是资深研发缺陷分析专家。系统已从知识库中检索出与当前缺陷相似的历史缺陷（含历史根因与解决方案）。\n"
                    + "请基于这些历史知识，为当前缺陷给出根因推测与解决建议，并说明每条历史知识与当前缺陷的关联点。\n"
                    + "要求：建议必须来源于给定的历史知识，不要编造不存在的接口、类名或配置项；"
                    + "若某条历史知识与当前缺陷关联度低，需在 summary 中明确指出。\n"
                    + "只输出一个 JSON 对象，不要输出任何解释文字，也不要使用 Markdown 代码块：\n"
                    + "{\"rootCauseGuess\":\"\",\"suggestedSolution\":\"\",\"summary\":\"\","
                    + "\"reasons\":[{\"knowledgeId\":1,\"reason\":\"关联点，30字以内\"}]}";

    private static final String COMPLETE_SYSTEM_PROMPT =
            "你是资深测试工程师。请根据缺陷标题与已知信息，补全为一份规范、可直接提交的缺陷描述。\n"
                    + "要求：复现步骤具体可执行并分步编号；预期结果与实际结果相互对照；"
                    + "不要编造具体的报错码、日志内容或版本号，信息不足处用【待补充】占位。\n"
                    + "只输出一个 JSON 对象，不要输出任何解释文字，也不要使用 Markdown 代码块：\n"
                    + "{\"description\":\"\",\"reproduceSteps\":\"\",\"expectedResult\":\"\",\"actualResult\":\"\","
                    + "\"environment\":\"\",\"questions\":[\"需要提交人补充确认的问题\"]}";

    private final DefectMapper defectMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final DeepSeekClient deepSeekClient;
    private final DeepSeekProperties deepSeekProperties;
    private final ChromaProperties chromaProperties;
    private final KnowledgeRetrievalService retrievalService;
    private final AiRuleEngine ruleEngine;

    // ---- 能力一：自动分类 + 优先级判定 ----

    @Override
    public AiClassifyVO classify(AiClassifyDTO dto) {
        Defect defect = dto.getDefectId() == null ? null : requireDefect(dto.getDefectId());
        String title = firstNonBlank(dto.getTitle(), defect == null ? null : defect.getTitle());
        String description = firstNonBlank(dto.getDescription(), defect == null ? null : defect.getDescription());
        String module = firstNonBlank(dto.getModule(), defect == null ? null : defect.getModule());
        if (StrUtil.isBlank(title)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺陷标题不能为空");
        }

        // 规则引擎结果同时作为兜底基准：大模型返回非法枚举值时直接沿用
        AiClassifyVO baseline = ruleEngine.classify(title, description);
        if (!deepSeekClient.isConfigured()) {
            return baseline;
        }
        try {
            ClassifyResult result = deepSeekClient.chatForJson(CLASSIFY_SYSTEM_PROMPT,
                    buildClassifyPrompt(title, description, module), ClassifyResult.class);
            AiClassifyVO vo = new AiClassifyVO();
            vo.setType(validType(result.getType()) ? result.getType() : baseline.getType());
            vo.setPriority(validPriority(result.getPriority()) ? result.getPriority() : baseline.getPriority());
            vo.setSeverity(validSeverity(result.getSeverity()) ? result.getSeverity() : baseline.getSeverity());
            vo.setTypeDesc(DefectTypeEnum.descOf(vo.getType()));
            vo.setPriorityDesc(DefectPriorityEnum.descOf(vo.getPriority()));
            vo.setSeverityDesc(DefectSeverityEnum.descOf(vo.getSeverity()));
            vo.setConfidence(normalizeConfidence(result.getConfidence(), 0.85));
            vo.setReason(StrUtil.blankToDefault(result.getReason(), "由 DeepSeek 模型判定"));
            vo.setSource("LLM");
            return vo;
        } catch (Exception e) {
            log.warn("DeepSeek 分类失败，降级为本地规则引擎: {}", e.getMessage());
            baseline.setReason(baseline.getReason() + "（AI 调用失败已降级）");
            return baseline;
        }
    }

    // ---- 能力二：RAG 根因/方案推荐 Top3 ----

    @Override
    public AiRecommendVO recommend(AiRecommendDTO dto) {
        Defect defect = dto.getDefectId() == null ? null : requireDefect(dto.getDefectId());
        String title = firstNonBlank(dto.getTitle(), defect == null ? null : defect.getTitle());
        String description = firstNonBlank(dto.getDescription(), defect == null ? null : defect.getDescription());
        String type = firstNonBlank(dto.getType(), defect == null ? null : defect.getType());
        if (StrUtil.isBlank(title) && StrUtil.isBlank(description)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺陷标题与描述不能同时为空");
        }

        AiRecommendVO vo = new AiRecommendVO();
        String query = buildQueryText(title, description);
        vo.setQuery(query);

        // 检索 Top3 历史知识（向量 → 全文索引 → 关键词，逐级降级）
        RetrievalResultVO retrieval = retrievalService.search(query, TOP_N);
        vo.setRetrievalMode(retrieval.getMode());
        vo.setCandidateCount(retrieval.getHits().size());
        vo.setRecommendations(toRecommendItems(retrieval.getHits()));

        if (retrieval.getHits().isEmpty()) {
            vo.setSource("NONE");
            vo.setSummary("知识库中暂未检索到相似缺陷，建议先完善该缺陷的复现步骤与实际结果，"
                    + "待问题解决后通过「沉淀知识」入口补充进知识库。");
            return vo;
        }

        if (!deepSeekClient.isConfigured()) {
            applyRuleSummary(vo, retrieval);
            return vo;
        }
        try {
            RecommendResult result = deepSeekClient.chatForJson(RECOMMEND_SYSTEM_PROMPT,
                    buildRecommendPrompt(title, description, type, retrieval), RecommendResult.class);
            vo.setRootCauseGuess(result.getRootCauseGuess());
            vo.setSuggestedSolution(result.getSuggestedSolution());
            vo.setSummary(result.getSummary());
            vo.setSource("LLM");
            applyReasons(vo.getRecommendations(), result.getReasons());
        } catch (Exception e) {
            log.warn("DeepSeek 推荐失败，降级为本地汇总: {}", e.getMessage());
            applyRuleSummary(vo, retrieval);
            vo.setSummary(StrUtil.blankToDefault(vo.getSummary(), "") + "（AI 调用失败已降级）");
        }
        return vo;
    }

    // ---- 能力三：描述补全提示 ----

    @Override
    public AiCompleteVO completeDescription(AiCompleteDTO dto) {
        Defect defect = dto.getDefectId() == null ? null : requireDefect(dto.getDefectId());
        String title = firstNonBlank(dto.getTitle(), defect == null ? null : defect.getTitle());
        String description = firstNonBlank(dto.getDescription(), defect == null ? null : defect.getDescription());
        String type = firstNonBlank(dto.getType(), defect == null ? null : defect.getType());
        String module = firstNonBlank(dto.getModule(), defect == null ? null : defect.getModule());
        String environment = firstNonBlank(dto.getEnvironment(), defect == null ? null : defect.getEnvironment());
        if (StrUtil.isBlank(title)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺陷标题不能为空");
        }

        if (!deepSeekClient.isConfigured()) {
            return ruleEngine.completeDescription(title, description, module, environment);
        }
        try {
            CompleteResult result = deepSeekClient.chatForJson(COMPLETE_SYSTEM_PROMPT,
                    buildCompletePrompt(title, description, type, module, environment), CompleteResult.class);

            String phenomenon = StrUtil.blankToDefault(result.getDescription(), description);
            String steps = result.getReproduceSteps();
            String expected = result.getExpectedResult();
            String actual = result.getActualResult();
            String env = StrUtil.blankToDefault(result.getEnvironment(), environment);

            List<AiSuggestionVO> suggestions = new ArrayList<>(4);
            suggestions.add(new AiSuggestionVO("reproduceSteps", "复现步骤", steps));
            suggestions.add(new AiSuggestionVO("expectedResult", "预期结果", expected));
            suggestions.add(new AiSuggestionVO("actualResult", "实际结果", actual));
            suggestions.add(new AiSuggestionVO("environment", "运行环境", env));

            AiCompleteVO vo = new AiCompleteVO();
            vo.setCompletedDescription(ruleEngine.buildDescription(
                    StrUtil.blankToDefault(phenomenon, ""),
                    StrUtil.blankToDefault(steps, ""),
                    StrUtil.blankToDefault(expected, ""),
                    StrUtil.blankToDefault(actual, ""),
                    StrUtil.blankToDefault(env, "")));
            vo.setSuggestions(suggestions);
            vo.setQuestions(result.getQuestions() == null ? Collections.emptyList() : result.getQuestions());
            vo.setSource("LLM");
            return vo;
        } catch (Exception e) {
            log.warn("DeepSeek 描述补全失败，降级为本地模板: {}", e.getMessage());
            AiCompleteVO vo = ruleEngine.completeDescription(title, description, module, environment);
            vo.setCompletedDescription(vo.getCompletedDescription() + "\n\n（AI 调用失败已降级为模板）");
            return vo;
        }
    }

    // ---- 状态自检与索引维护 ----

    @Override
    public AiStatusVO status() {
        boolean chromaReachable = retrievalService.vectorAvailable();
        AiStatusVO vo = new AiStatusVO();
        vo.setDeepseekConfigured(deepSeekClient.isConfigured());
        vo.setDeepseekReachable(deepSeekClient.ping());
        vo.setModel(deepSeekProperties.getModel());
        vo.setChromaReachable(chromaReachable);
        vo.setChromaCollection(chromaProperties.getCollection());
        vo.setVectorCount(retrievalService.vectorCount());
        vo.setKnowledgeCount(knowledgeMapper.selectCount(null));
        // 向量库可用则走语义检索，否则 RAG 自动落在 MySQL 全文索引上
        vo.setRetrievalMode(chromaReachable ? "VECTOR" : "FULLTEXT");
        return vo;
    }

    @Override
    public int rebuildIndex() {
        return retrievalService.rebuildAll();
    }

    // ---- 私有：大模型输入组装 ----

    private String buildClassifyPrompt(String title, String description, String module) {
        StringBuilder sb = new StringBuilder();
        sb.append("【缺陷标题】\n").append(title);
        if (StrUtil.isNotBlank(module)) {
            sb.append("\n\n【所属模块】\n").append(module);
        }
        if (StrUtil.isNotBlank(description)) {
            sb.append("\n\n【缺陷描述】\n").append(StrUtil.maxLength(description, MAX_PROMPT_DESC_LENGTH));
        }
        return sb.append("\n\n请给出分类结果。").toString();
    }

    private String buildRecommendPrompt(String title, String description, String type, RetrievalResultVO retrieval) {
        StringBuilder sb = new StringBuilder("【当前缺陷】\n");
        sb.append("标题：").append(StrUtil.blankToDefault(title, "无")).append('\n');
        sb.append("类型：").append(StrUtil.blankToDefault(DefectTypeEnum.descOf(type), "未指定")).append('\n');
        sb.append("描述：").append(StrUtil.blankToDefault(
                StrUtil.maxLength(description, MAX_PROMPT_DESC_LENGTH), "无"));

        sb.append("\n\n【知识库检索到的历史缺陷】（检索方式：").append(retrieval.getMode()).append("）\n");
        List<RetrievalHitVO> hits = retrieval.getHits();
        for (int i = 0; i < hits.size(); i++) {
            RetrievalHitVO hit = hits.get(i);
            sb.append(i + 1).append(". knowledgeId=").append(hit.getKnowledgeId()).append('\n');
            sb.append("   标题：").append(StrUtil.blankToDefault(hit.getTitle(), "无")).append('\n');
            sb.append("   根因：").append(StrUtil.blankToDefault(hit.getRootCause(), "未记录")).append('\n');
            sb.append("   解决方案：").append(StrUtil.blankToDefault(hit.getSolution(), "未记录")).append('\n');
        }
        return sb.append("\n请给出根因推测与解决建议。").toString();
    }

    private String buildCompletePrompt(String title, String description, String type,
                                       String module, String environment) {
        StringBuilder sb = new StringBuilder("【缺陷标题】\n").append(title);
        if (StrUtil.isNotBlank(type)) {
            sb.append("\n\n【缺陷类型】\n").append(DefectTypeEnum.descOf(type));
        }
        if (StrUtil.isNotBlank(module)) {
            sb.append("\n\n【所属模块】\n").append(module);
        }
        if (StrUtil.isNotBlank(environment)) {
            sb.append("\n\n【运行环境】\n").append(environment);
        }
        if (StrUtil.isNotBlank(description)) {
            sb.append("\n\n【已填写描述】\n").append(StrUtil.maxLength(description, MAX_PROMPT_DESC_LENGTH));
        }
        return sb.append("\n\n请补全描述。").toString();
    }

    // ---- 私有：结果组装 ----

    private List<AiRecommendItemVO> toRecommendItems(List<RetrievalHitVO> hits) {
        List<AiRecommendItemVO> items = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            RetrievalHitVO hit = hits.get(i);
            AiRecommendItemVO item = new AiRecommendItemVO();
            item.setKnowledgeId(hit.getKnowledgeId());
            item.setTitle(hit.getTitle());
            item.setType(hit.getType());
            item.setTypeDesc(hit.getTypeDesc());
            item.setRootCause(hit.getRootCause());
            item.setSolution(hit.getSolution());
            item.setTagList(hit.getTagList());
            item.setScore(hit.getScore());
            item.setMatchedBy(hit.getMatchedBy());
            item.setReason(defaultReason(hit, i + 1));
            items.add(item);
        }
        return items;
    }

    /**
     * 无大模型时的推荐理由：用命中方式与相关度拼一句可读的说明
     */
    private String defaultReason(RetrievalHitVO hit, int rank) {
        String modeText;
        if ("VECTOR".equals(hit.getMatchedBy())) {
            modeText = "向量语义相似";
        } else if ("FULLTEXT".equals(hit.getMatchedBy())) {
            modeText = "全文索引命中";
        } else {
            modeText = "关键词匹配";
        }
        String scoreText = hit.getScore() == null ? "" : "，相关度 " + hit.getScore();
        return "第 " + rank + " 位推荐：" + modeText + scoreText;
    }

    /**
     * 用大模型给出的关联说明覆盖默认推荐理由
     */
    private void applyReasons(List<AiRecommendItemVO> items, List<ReasonItem> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return;
        }
        Map<Long, String> reasonMap = new LinkedHashMap<>();
        for (ReasonItem item : reasons) {
            if (item.getKnowledgeId() != null && StrUtil.isNotBlank(item.getReason())) {
                reasonMap.put(item.getKnowledgeId(), item.getReason());
            }
        }
        for (AiRecommendItemVO item : items) {
            String reason = reasonMap.get(item.getKnowledgeId());
            if (StrUtil.isNotBlank(reason)) {
                item.setReason(reason);
            }
        }
    }

    /**
     * 本地降级汇总：直接引用最相关的一条历史知识，避免无大模型时前端出现空白
     */
    private void applyRuleSummary(AiRecommendVO vo, RetrievalResultVO retrieval) {
        List<RetrievalHitVO> hits = retrieval.getHits();
        RetrievalHitVO best = hits.get(0);
        vo.setRootCauseGuess(StrUtil.blankToDefault(best.getRootCause(),
                "历史知识未记录根因，建议结合日志与代码定位"));
        vo.setSuggestedSolution(StrUtil.blankToDefault(best.getSolution(),
                "历史知识未记录解决方案，建议参考同类缺陷处理思路"));
        vo.setSummary("共检索到 " + hits.size() + " 条相似历史缺陷（检索方式：" + retrieval.getMode() + "）。"
                + "其中最相关的是「" + StrUtil.blankToDefault(best.getTitle(), "无标题") + "」，"
                + "根因记录为：" + vo.getRootCauseGuess() + "；解决方案记录为：" + vo.getSuggestedSolution()
                + "。建议优先按该方案排查，若现象不完全一致，可继续参考其余 "
                + (hits.size() - 1) + " 条历史缺陷。");
        vo.setSource("RULE");
    }

    private String buildQueryText(String title, String description) {
        String text = StrUtil.blankToDefault(title, "") + " " + StrUtil.blankToDefault(description, "");
        return StrUtil.maxLength(text.trim(), MAX_QUERY_LENGTH);
    }

    // ---- 私有：校验与取值 ----

    private Defect requireDefect(Long defectId) {
        Defect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        return defect;
    }

    private String firstNonBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : second;
    }

    /**
     * 校验大模型返回的枚举代码是否合法（descOf 未命中时会原样返回入参，据此判断）
     */
    private boolean validType(String code) {
        return StrUtil.isNotBlank(code) && !code.equals(DefectTypeEnum.descOf(code));
    }

    private boolean validPriority(String code) {
        return StrUtil.isNotBlank(code) && !code.equals(DefectPriorityEnum.descOf(code));
    }

    private boolean validSeverity(String code) {
        return StrUtil.isNotBlank(code) && !code.equals(DefectSeverityEnum.descOf(code));
    }

    /** 大模型给出的置信度可能越界或缺失，统一收敛到 0.1-1.0 */
    private Double normalizeConfidence(Double confidence, double defaultValue) {
        if (confidence == null || confidence <= 0) {
            return defaultValue;
        }
        return Math.min(1.0, Math.max(0.1, confidence));
    }

    // ---- 私有：大模型返回结构 ----

    /** 分类结果反序列化载体 */
    @Data
    public static class ClassifyResult {
        private String type;
        private String priority;
        private String severity;
        private Double confidence;
        private String reason;
    }

    /** 推荐结果反序列化载体 */
    @Data
    public static class RecommendResult {
        private String rootCauseGuess;
        private String suggestedSolution;
        private String summary;
        private List<ReasonItem> reasons;
    }

    /** 单条推荐理由 */
    @Data
    public static class ReasonItem {
        private Long knowledgeId;
        private String reason;
    }

    /** 描述补全结果反序列化载体 */
    @Data
    public static class CompleteResult {
        private String description;
        private String reproduceSteps;
        private String expectedResult;
        private String actualResult;
        private String environment;
        private List<String> questions;
    }
}
