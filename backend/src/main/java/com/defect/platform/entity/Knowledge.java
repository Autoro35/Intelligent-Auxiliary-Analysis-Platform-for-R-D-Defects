package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体，对应表 t_knowledge
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge")
public class Knowledge extends BaseEntity {

    /** 知识标题 */
    private String title;

    /** 来源缺陷 ID */
    private Long defectId;

    /** 缺陷类型 */
    private String type;

    /** 根因分类 */
    private String rootCause;

    /** 解决方案 */
    private String solution;

    /** 标签（逗号分隔） */
    private String tags;

    /** 浏览次数 */
    private Integer viewCount;

    /** 创建人用户 ID */
    private Long createBy;
}
