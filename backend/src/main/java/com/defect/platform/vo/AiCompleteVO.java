package com.defect.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 描述补全结果
 */
@Data
public class AiCompleteVO {

    /** 整合后的完整缺陷描述，可直接回填描述框 */
    private String completedDescription;

    /** 分字段建议（复现步骤 / 预期结果 / 实际结果 / 运行环境等） */
    private List<AiSuggestionVO> suggestions;

    /** 需要提交人补充确认的问题清单 */
    private List<String> questions;

    /** 结果来源：LLM（大模型）/ RULE（本地规则降级） */
    private String source;
}
