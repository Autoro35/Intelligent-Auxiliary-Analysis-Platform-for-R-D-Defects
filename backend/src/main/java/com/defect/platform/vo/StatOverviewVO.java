package com.defect.platform.vo;

import lombok.Data;

/**
 * 统计总览（仪表盘顶部指标卡）
 */
@Data
public class StatOverviewVO {

    /** 缺陷总数 */
    private Long total;

    /** 未关闭数量（不含已关闭） */
    private Long open;

    /** 新建数量 */
    private Long newCount;

    /** 已分配数量 */
    private Long assigned;

    /** 处理中数量 */
    private Long processing;

    /** 待复测数量 */
    private Long pendingRetest;

    /** 已关闭数量 */
    private Long closed;

    /** 已驳回数量 */
    private Long rejected;

    /** 关闭率（百分比，保留 1 位小数） */
    private Double completionRate;

    /** 项目数量 */
    private Long projectCount;

    /** 知识库条目数量 */
    private Long knowledgeCount;
}
