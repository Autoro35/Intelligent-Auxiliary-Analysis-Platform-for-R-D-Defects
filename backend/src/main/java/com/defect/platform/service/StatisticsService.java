package com.defect.platform.service;

import com.defect.platform.vo.MemberWorkloadVO;
import com.defect.platform.vo.StatDistributionVO;
import com.defect.platform.vo.StatOverviewVO;
import com.defect.platform.vo.StatTrendVO;

import java.util.List;

/**
 * 统计服务：总览、分布、趋势、个人工作量
 * <p>统计范围遵循多项目隔离：管理员看全部，其余用户仅看可见项目的缺陷</p>
 */
public interface StatisticsService {

    StatOverviewVO overview();

    StatDistributionVO distribution();

    List<StatTrendVO> trend(int days);

    List<MemberWorkloadVO> workload();
}
