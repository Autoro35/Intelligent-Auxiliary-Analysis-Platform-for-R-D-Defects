package com.defect.platform.vo;

import lombok.Data;

/**
 * AI 自动分类与优先级判定结果
 */
@Data
public class AiClassifyVO {

    /** 缺陷类型：FUNCTIONAL/PERFORMANCE/UI/COMPATIBILITY/OPTIMIZATION */
    private String type;

    /** 类型中文描述 */
    private String typeDesc;

    /** 优先级：URGENT/HIGH/MEDIUM/LOW */
    private String priority;

    /** 优先级中文描述 */
    private String priorityDesc;

    /** 严重程度：BLOCKER/CRITICAL/MAJOR/MINOR */
    private String severity;

    /** 严重程度中文描述 */
    private String severityDesc;

    /** 置信度（0-1） */
    private Double confidence;

    /** 判定依据 */
    private String reason;

    /** 结果来源：LLM（大模型）/ RULE（本地规则降级） */
    private String source;
}
