package com.defect.platform.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户信息，由 JWT 解析后放入上下文
 */
@Data
@AllArgsConstructor
public class LoginUser {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色（ADMIN/TESTER/DEVELOPER/GUEST） */
    private String role;
}
