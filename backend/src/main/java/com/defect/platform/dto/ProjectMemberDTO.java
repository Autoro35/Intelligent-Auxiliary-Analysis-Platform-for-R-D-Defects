package com.defect.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目成员添加请求参数
 */
@Data
public class ProjectMemberDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "项目内角色不能为空")
    private String role;
}
