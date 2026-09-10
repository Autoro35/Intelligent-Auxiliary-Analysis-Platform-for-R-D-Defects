package com.defect.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "用户名需为3-32位字母、数字或下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需为6-64位")
    private String password;

    @Size(max = 32, message = "昵称最长32个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 角色（可选）：ADMIN/TESTER/DEVELOPER/GUEST，缺省为 GUEST */
    private String role;
}
