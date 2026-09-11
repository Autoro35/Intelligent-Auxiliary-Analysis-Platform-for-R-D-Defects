package com.defect.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * 缺陷四维度分布（状态/类型/优先级/严重程度），供仪表盘饼图/柱状图使用
 */
@Data
public class StatDistributionVO {

    /** 状态分布 */
    private List<StatItemVO> status;

    /** 类型分布 */
    private List<StatItemVO> type;

    /** 优先级分布 */
    private List<StatItemVO> priority;

    /** 严重程度分布 */
    private List<StatItemVO> severity;
}
