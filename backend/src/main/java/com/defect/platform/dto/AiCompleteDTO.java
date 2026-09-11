package com.defect.platform.dto;

import lombok.Data;

/**
 * AI 描述补全请求
 */
@Data
public class AiCompleteDTO {

    /** 已存在的缺陷 ID，可选 */
    private Long defectId;

    /** 缺陷标题 */
    private String title;

    /** 已填写的缺陷描述（可为空，由 AI 起草） */
    private String description;

    /** 缺陷类型，可选 */
    private String type;

    /** 所属模块，可选 */
    private String module;

    /** 运行环境，可选 */
    private String environment;
}
