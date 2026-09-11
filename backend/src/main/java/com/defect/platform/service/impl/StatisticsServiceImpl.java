package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.defect.platform.common.constant.DefectPriorityEnum;
import com.defect.platform.common.constant.DefectSeverityEnum;
import com.defect.platform.common.constant.DefectStatusEnum;
import com.defect.platform.common.constant.DefectTypeEnum;
import com.defect.platform.entity.Defect;
import com.defect.platform.entity.User;
import com.defect.platform.mapper.DefectMapper;
import com.defect.platform.mapper.KnowledgeMapper;
import com.defect.platform.mapper.UserMapper;
import com.defect.platform.service.ProjectService;
import com.defect.platform.service.StatisticsService;
import com.defect.platform.vo.MemberWorkloadVO;
import com.defect.platform.vo.StatDistributionVO;
import com.defect.platform.vo.StatItemVO;
import com.defect.platform.vo.StatOverviewVO;
import com.defect.platform.vo.StatTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统计服务实现
 * <p>基于内存聚合：一次性加载可见项目的缺陷（仅必要列），避免多次 SQL；数据量对 10-30 人团队完全可控</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final DefectMapper defectMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final UserMapper userMapper;
    private final ProjectService projectService;

    @Override
    public StatOverviewVO overview() {
        List<Defect> defects = loadAccessibleDefects();

        long total = defects.size();
        long closed = countStatus(defects, DefectStatusEnum.CLOSED.getCode());
        long rejected = countStatus(defects, DefectStatusEnum.REJECTED.getCode());
        long newCount = countStatus(defects, DefectStatusEnum.NEW.getCode());
        long assigned = countStatus(defects, DefectStatusEnum.ASSIGNED.getCode());
        long processing = countStatus(defects, DefectStatusEnum.PROCESSING.getCode());
        long pendingRetest = countStatus(defects, DefectStatusEnum.PENDING_RETEST.getCode());
        // 关闭率：保留 1 位小数
        double completionRate = total == 0 ? 0 : Math.round(closed * 1000.0 / total) / 10.0;

        StatOverviewVO vo = new StatOverviewVO();
        vo.setTotal(total);
        vo.setOpen(total - closed);
        vo.setNewCount(newCount);
        vo.setAssigned(assigned);
        vo.setProcessing(processing);
        vo.setPendingRetest(pendingRetest);
        vo.setClosed(closed);
        vo.setRejected(rejected);
        vo.setCompletionRate(completionRate);
        vo.setProjectCount(countAccessibleProjects());
        vo.setKnowledgeCount(knowledgeMapper.selectCount(null));
        return vo;
    }

    @Override
    public StatDistributionVO distribution() {
        List<Defect> defects = loadAccessibleDefects();
        StatDistributionVO vo = new StatDistributionVO();
        vo.setStatus(groupBy(defects, Defect::getStatus, DefectStatusEnum.values()));
        vo.setType(groupBy(defects, Defect::getType, DefectTypeEnum.values()));
        vo.setPriority(groupBy(defects, Defect::getPriority, DefectPriorityEnum.values()));
        vo.setSeverity(groupBy(defects, Defect::getSeverity, DefectSeverityEnum.values()));
        return vo;
    }

    @Override
    public List<StatTrendVO> trend(int days) {
        int n = Math.max(1, Math.min(days, 365));
        LocalDate start = LocalDate.now().minusDays(n - 1);
        Set<Long> ids = accessibleProjectIds();
        if (ids != null && ids.isEmpty()) {
            return emptyTrend(start, n);
        }
        Map<LocalDate, Long> created = countByDay(Defect::getCreateTime, start, ids);
        Map<LocalDate, Long> closed = countByDay(Defect::getClosedTime, start, ids);

        List<StatTrendVO> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LocalDate day = start.plusDays(i);
            StatTrendVO vo = new StatTrendVO();
            vo.setDate(day.toString());
            vo.setCreated(created.getOrDefault(day, 0L));
            vo.setClosed(closed.getOrDefault(day, 0L));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<MemberWorkloadVO> workload() {
        List<Defect> defects = loadAccessibleDefects();
        Map<Long, List<Defect>> byAssignee = defects.stream()
                .filter(d -> d.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Defect::getAssigneeId));
        if (byAssignee.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> users = userMapper.selectBatchIds(byAssignee.keySet()).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<MemberWorkloadVO> result = new ArrayList<>(byAssignee.size());
        for (Map.Entry<Long, List<Defect>> e : byAssignee.entrySet()) {
            List<Defect> list = e.getValue();
            MemberWorkloadVO vo = new MemberWorkloadVO();
            vo.setUserId(e.getKey());
            User u = users.get(e.getKey());
            vo.setName(u == null ? null : StrUtil.blankToDefault(u.getNickname(), u.getUsername()));
            vo.setTotal((long) list.size());
            vo.setClosed(countStatus(list, DefectStatusEnum.CLOSED.getCode()));
            vo.setProcessing(countStatus(list, DefectStatusEnum.PROCESSING.getCode()));
            vo.setPendingRetest(countStatus(list, DefectStatusEnum.PENDING_RETEST.getCode()));
            vo.setResolved(list.stream().filter(d -> d.getResolvedTime() != null).count());
            vo.setReopenCount(list.stream()
                    .mapToLong(d -> d.getReopenCount() == null ? 0 : d.getReopenCount())
                    .sum());
            result.add(vo);
        }
        result.sort(Comparator.comparing(MemberWorkloadVO::getTotal).reversed());
        return result;
    }

    // ---- 数据加载 ----

    /** 可见项目 ID 集合，管理员为 null 表示不限 */
    private Set<Long> accessibleProjectIds() {
        return projectService.isAdmin() ? null : projectService.getAccessibleProjectIds();
    }

    private List<Defect> loadAccessibleDefects() {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<Defect>()
                .select(Defect::getStatus, Defect::getType, Defect::getPriority, Defect::getSeverity,
                        Defect::getAssigneeId, Defect::getResolvedTime, Defect::getReopenCount);
        Set<Long> ids = accessibleProjectIds();
        if (ids != null) {
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Defect::getProjectId, ids);
        }
        return defectMapper.selectList(wrapper);
    }

    /** 按某时间列按天聚合数量 */
    private Map<LocalDate, Long> countByDay(SFunction<Defect, LocalDateTime> column, LocalDate start, Set<Long> ids) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<Defect>()
                .select(column)
                .ge(column, start.atStartOfDay());
        if (ids != null) {
            wrapper.in(Defect::getProjectId, ids);
        }
        return defectMapper.selectList(wrapper).stream()
                .map(column)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private long countAccessibleProjects() {
        return projectService.isAdmin()
                ? projectService.count()
                : projectService.getAccessibleProjectIds().size();
    }

    // ---- 聚合工具 ----

    /** 按枚举维度聚合，缺失维度补 0，保证前端图例完整 */
    private <E extends Enum<E>> List<StatItemVO> groupBy(List<Defect> defects,
                                                         Function<Defect, String> extractor,
                                                         E[] values) {
        Map<String, Long> counts = defects.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<StatItemVO> items = new ArrayList<>(values.length);
        for (E e : values) {
            String code = codeOf(e);
            items.add(new StatItemVO(code, descOf(e), counts.getOrDefault(code, 0L)));
        }
        return items;
    }

    private long countStatus(List<Defect> defects, String status) {
        return defects.stream().filter(d -> status.equals(d.getStatus())).count();
    }

    private List<StatTrendVO> emptyTrend(LocalDate start, int n) {
        List<StatTrendVO> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            StatTrendVO vo = new StatTrendVO();
            vo.setDate(start.plusDays(i).toString());
            vo.setCreated(0L);
            vo.setClosed(0L);
            result.add(vo);
        }
        return result;
    }

    // 统一从枚举取 code/desc（避免各维度重复逻辑）
    private static String codeOf(Enum<?> e) {
        if (e instanceof DefectStatusEnum) {
            return ((DefectStatusEnum) e).getCode();
        }
        if (e instanceof DefectTypeEnum) {
            return ((DefectTypeEnum) e).getCode();
        }
        if (e instanceof DefectPriorityEnum) {
            return ((DefectPriorityEnum) e).getCode();
        }
        if (e instanceof DefectSeverityEnum) {
            return ((DefectSeverityEnum) e).getCode();
        }
        return e.name();
    }

    private static String descOf(Enum<?> e) {
        if (e instanceof DefectStatusEnum) {
            return ((DefectStatusEnum) e).getDesc();
        }
        if (e instanceof DefectTypeEnum) {
            return ((DefectTypeEnum) e).getDesc();
        }
        if (e instanceof DefectPriorityEnum) {
            return ((DefectPriorityEnum) e).getDesc();
        }
        if (e instanceof DefectSeverityEnum) {
            return ((DefectSeverityEnum) e).getDesc();
        }
        return e.name();
    }
}
