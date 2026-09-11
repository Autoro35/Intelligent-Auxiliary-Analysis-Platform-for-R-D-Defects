package com.defect.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述补全的单字段建议
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionVO {

    /** 字段名（与 DefectDTO 字段严格对齐） */
    private String field;

    /** 字段中文标签 */
    private String label;

    /** 建议内容 */
    private String content;
}
