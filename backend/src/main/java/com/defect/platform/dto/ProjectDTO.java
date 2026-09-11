package com.defect.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目创建/编辑请求参数
 */
@Data
public class ProjectDTO {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称最长100字符")
    private String name;

    @NotBlank(message = "项目编码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,50}$", message = "项目编码需为2-50位字母、数字、下划线或中划线")
    private String code;

    @Size(max = 500, message = "项目描述最长500字符")
    private String description;

    /** 项目负责人用户 ID，创建时缺省为当前用户 */
    private Long ownerId;

    /** 状态：1-进行中 0-已归档，缺省 1 */
    private Integer status;
}
