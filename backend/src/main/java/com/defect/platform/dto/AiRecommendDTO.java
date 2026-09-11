package com.defect.platform.dto;

import lombok.Data;

/**
 * RAG 根因/方案推荐请求
 * <p>两种用法：传 defectId 由后端补齐信息；或直接传 title/description（新建缺陷表单场景）</p>
 */
@Data
public class AiRecommendDTO {

    /** 已存在的缺陷 ID，可选 */
    private Long defectId;

    /** 缺陷标题 */
    private String title;

    /** 缺陷描述 */
    private String description;

    /** 缺陷类型，可选（缩小检索范围） */
    private String type;
}
