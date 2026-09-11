package com.defect.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 缺陷创建/编辑请求参数
 * <p>状态与处理人不在编辑范围，通过流转接口（transition）变更</p>
 */
@Data
public class DefectDTO {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotBlank(message = "缺陷标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    private String description;

    /** 缺陷类型：FUNCTIONAL/PERFORMANCE/UI/COMPATIBILITY/OPTIMIZATION */
    private String type;

    /** 优先级：URGENT/HIGH/MEDIUM/LOW */
    private String priority;

    /** 严重程度：BLOCKER/CRITICAL/MAJOR/MINOR */
    private String severity;

    private String module;

    private String environment;

    private String reproduceSteps;

    private String expectedResult;

    private String actualResult;

    private String solution;

    private String rootCause;
}
