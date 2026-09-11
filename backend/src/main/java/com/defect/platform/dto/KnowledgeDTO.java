package com.defect.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库创建/编辑请求参数
 */
@Data
public class KnowledgeDTO {

    @NotBlank(message = "知识标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    /** 来源缺陷 ID（沉淀时关联，可空） */
    private Long defectId;

    /** 缺陷类型：FUNCTIONAL/PERFORMANCE/UI/COMPATIBILITY/OPTIMIZATION */
    private String type;

    /** 根因分类 */
    @Size(max = 100, message = "根因分类最长100字符")
    private String rootCause;

    /** 解决方案 */
    private String solution;

    /** 标签，逗号分隔 */
    @Size(max = 255, message = "标签最长255字符")
    private String tags;
}
