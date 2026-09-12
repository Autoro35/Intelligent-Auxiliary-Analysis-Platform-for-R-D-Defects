package com.defect.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员维护用户信息的入参
 * <p>用户名是登录凭证，不开放修改</p>
 */
@Data
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 角色：ADMIN/TESTER/DEVELOPER/GUEST */
    private String role;

    /** 状态：1-启用 0-禁用 */
    private Integer status;
}
