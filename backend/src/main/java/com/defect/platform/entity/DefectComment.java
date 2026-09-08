package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 缺陷评论实体，对应表 t_defect_comment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_defect_comment")
public class DefectComment extends BaseEntity {

    /** 缺陷 ID */
    private Long defectId;

    /** 评论人用户 ID */
    private Long userId;

    /** 评论内容 */
    private String content;
}
