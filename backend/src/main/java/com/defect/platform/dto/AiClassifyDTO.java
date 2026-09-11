package com.defect.platform.dto;

import lombok.Data;

/**
 * AI 自动分类请求
 * <p>两种用法：传 defectId 由后端补齐标题/描述；或直接传 title/description（新建缺陷表单场景）</p>
 */
@Data
public class AiClassifyDTO {

    /** 已存在的缺陷 ID，可选 */
    private Long defectId;

    /** 缺陷标题 */
    private String title;

    /** 缺陷描述 */
    private String description;

    /** 所属模块，辅助判定 */
    private String module;
}
