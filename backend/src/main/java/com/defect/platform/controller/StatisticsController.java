package com.defect.platform.controller;

import com.defect.platform.common.Result;
import com.defect.platform.service.StatisticsService;
import com.defect.platform.vo.MemberWorkloadVO;
import com.defect.platform.vo.StatDistributionVO;
import com.defect.platform.vo.StatOverviewVO;
import com.defect.platform.vo.StatTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口：总览、分布、趋势、个人工作量（登录即可访问，遵循多项目隔离）
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<StatOverviewVO> overview() {
        return Result.success(statisticsService.overview());
    }

    @GetMapping("/distribution")
    public Result<StatDistributionVO> distribution() {
        return Result.success(statisticsService.distribution());
    }

    @GetMapping("/trend")
    public Result<List<StatTrendVO>> trend(@RequestParam(defaultValue = "30") int days) {
        return Result.success(statisticsService.trend(days));
    }

    @GetMapping("/workload")
    public Result<List<MemberWorkloadVO>> workload() {
        return Result.success(statisticsService.workload());
    }
}
