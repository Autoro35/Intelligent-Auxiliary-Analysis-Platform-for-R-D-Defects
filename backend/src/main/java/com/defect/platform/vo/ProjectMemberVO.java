package com.defect.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员视图对象
 */
@Data
public class ProjectMemberVO {

    private Long userId;

    private String username;

    private String nickname;

    /** 项目内角色编码 */
    private String role;

    /** 项目内角色中文描述 */
    private String roleDesc;

    /** 加入时间 */
    private LocalDateTime createTime;
}
