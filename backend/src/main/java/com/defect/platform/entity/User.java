package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体，对应表 t_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    /** 登录用户名 */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 角色：ADMIN/TESTER/DEVELOPER/GUEST */
    private String role;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 头像 URL */
    private String avatar;
}
