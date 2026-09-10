package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 缺陷实体，对应表 t_defect（核心业务表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_defect")
public class Defect extends BaseEntity {

    /** 所属项目 ID */
    private Long projectId;

    /** 缺陷标题 */
    private String title;

    /** 缺陷描述 */
    private String description;

    /** 缺陷类型：FUNCTIONAL/PERFORMANCE/UI/COMPATIBILITY/OPTIMIZATION */
    private String type;

    /** 优先级：URGENT/HIGH/MEDIUM/LOW */
    private String priority;

    /** 严重程度：BLOCKER/CRITICAL/MAJOR/MINOR */
    private String severity;

    /** 状态：NEW/ASSIGNED/PROCESSING/PENDING_RETEST/CLOSED/REJECTED */
    private String status;

    /** 提交人用户 ID */
    private Long reporterId;

    /** 当前处理人用户 ID */
    private Long assigneeId;

    /** 所属模块 */
    private String module;

    /** 运行环境 */
    private String environment;

    /** 复现步骤 */
    private String reproduceSteps;

    /** 预期结果 */
    private String expectedResult;

    /** 实际结果 */
    private String actualResult;

    /** 解决方案 */
    private String solution;

    /** 根因分类 */
    private String rootCause;

    /** 重新打开次数 */
    private Integer reopenCount;

    /** 解决时间 */
    private LocalDateTime resolvedTime;

    /** 关闭时间 */
    private LocalDateTime closedTime;
}
