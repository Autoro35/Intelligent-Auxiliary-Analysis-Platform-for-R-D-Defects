package com.defect.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目视图对象
 */
@Data
public class ProjectVO {

    private Long id;

    private String name;

    private String code;

    private String description;

    /** 状态：1-进行中 0-已归档 */
    private Integer status;

    private Long ownerId;

    private String ownerName;

    /** 成员数量 */
    private Long memberCount;

    private LocalDateTime createTime;
}
