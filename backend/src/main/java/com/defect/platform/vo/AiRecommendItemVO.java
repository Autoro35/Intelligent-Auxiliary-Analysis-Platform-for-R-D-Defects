package com.defect.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * RAG 推荐的单条知识
 */
@Data
public class AiRecommendItemVO {

    /** 知识 ID */
    private Long knowledgeId;

    /** 知识标题 */
    private String title;

    /** 缺陷类型 */
    private String type;

    /** 类型中文描述 */
    private String typeDesc;

    /** 历史根因 */
    private String rootCause;

    /** 历史解决方案 */
    private String solution;

    /** 标签列表 */
    private List<String> tagList;

    /** 相关度（0-1，越大越相关） */
    private Double score;

    /** 推荐理由 */
    private String reason;

    /** 命中方式：VECTOR（向量）/ FULLTEXT（全文索引）/ KEYWORD（模糊匹配） */
    private String matchedBy;
}
