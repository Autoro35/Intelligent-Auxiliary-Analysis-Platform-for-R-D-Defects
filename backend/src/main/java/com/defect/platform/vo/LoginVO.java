package com.defect.platform.vo;

import lombok.Data;

/**
 * 登录成功响应：token + 用户信息
 */
@Data
public class LoginVO {

    private String token;

    private UserVO user;
}
