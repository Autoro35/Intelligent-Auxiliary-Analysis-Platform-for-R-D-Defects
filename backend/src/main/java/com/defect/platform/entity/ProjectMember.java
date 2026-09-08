package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目成员实体，对应表 t_project_member
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_project_member")
public class ProjectMember extends BaseEntity {

    /** 项目 ID */
    private Long projectId;

    /** 用户 ID */
    private Long userId;

    /** 项目内角色：OWNER/DEV/TESTER/VIEWER */
    private String role;
}
