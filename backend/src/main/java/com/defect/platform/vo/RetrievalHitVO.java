package com.defect.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * 知识检索命中项（AI 检索层内部使用）
 */
@Data
public class RetrievalHitVO {

    private Long knowledgeId;

    private String title;

    private String type;

    private String typeDesc;

    private String rootCause;

    private String solution;

    private String tags;

    private List<String> tagList;

    /** 相关度（0-1，越大越相关） */
    private Double score;

    /** 命中方式：VECTOR / FULLTEXT / KEYWORD */
    private String matchedBy;
}
