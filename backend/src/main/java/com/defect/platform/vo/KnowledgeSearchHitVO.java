package com.defect.platform.vo;

import lombok.Data;

/**
 * ngram 全文索引检索的原始命中项（由 KnowledgeMapper 直接映射）
 */
@Data
public class KnowledgeSearchHitVO {

    private Long id;

    private String title;

    private String type;

    private String rootCause;

    private String solution;

    private String tags;

    /** MySQL MATCH 相关度原始分（越大越相关） */
    private Double score;
}
