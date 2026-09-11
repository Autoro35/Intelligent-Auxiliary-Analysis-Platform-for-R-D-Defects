package com.defect.platform.vo;

import lombok.Data;

/**
 * 缺陷趋势点（按天统计新增与关闭数量）
 */
@Data
public class StatTrendVO {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 当日新增数量 */
    private Long created;

    /** 当日关闭数量 */
    private Long closed;
}
