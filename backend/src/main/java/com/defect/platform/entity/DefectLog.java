package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷操作日志实体，对应表 t_defect_log
 * <p>只追加、不逻辑删除，保证操作留痕</p>
 */
@Data
@TableName("t_defect_log")
public class DefectLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 缺陷 ID */
    private Long defectId;

    /** 操作人用户 ID */
    private Long operatorId;

    /** 操作类型：CREATE/UPDATE/ASSIGN/START/RESOLVE/RETEST/CLOSE/REJECT/COMMENT */
    private String action;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作备注 */
    private String remark;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
