package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目实体，对应表 t_project
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_project")
public class Project extends BaseEntity {

    /** 项目名称 */
    private String name;

    /** 项目编码（唯一） */
    private String code;

    /** 项目描述 */
    private String description;

    /** 状态：1-进行中 0-已归档 */
    private Integer status;

    /** 项目负责人用户 ID */
    private Long ownerId;
}
