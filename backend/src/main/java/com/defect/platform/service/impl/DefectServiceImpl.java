package com.defect.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defect.platform.common.PageResult;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.constant.DefectActionEnum;
import com.defect.platform.common.constant.DefectStatusEnum;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.dto.DefectCommentDTO;
import com.defect.platform.dto.DefectDTO;
import com.defect.platform.dto.DefectTransitionDTO;
import com.defect.platform.entity.Defect;
import com.defect.platform.entity.DefectComment;
import com.defect.platform.entity.DefectLog;
import com.defect.platform.entity.User;
import com.defect.platform.mapper.DefectCommentMapper;
import com.defect.platform.mapper.DefectLogMapper;
import com.defect.platform.mapper.DefectMapper;
import com.defect.platform.mapper.UserMapper;
import com.defect.platform.service.DefectService;
import com.defect.platform.service.ProjectService;
import com.defect.platform.vo.DefectCommentVO;
import com.defect.platform.vo.DefectLogVO;
import com.defect.platform.vo.DefectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缺陷服务实现
 * <p>状态流转：NEW→ASSIGNED→PROCESSING→PENDING_RETEST→CLOSED/REJECTED，REJECTED 可重新打开</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefectServiceImpl extends ServiceImpl<DefectMapper, Defect> implements DefectService {

    private final ProjectService projectService;
    private final DefectCommentMapper commentMapper;
    private final DefectLogMapper logMapper;
    private final UserMapper userMapper;

    @Override
    public DefectVO create(DefectDTO dto) {
        projectService.assertMember(dto.getProjectId());
        Defect defect = new Defect();
        BeanUtil.copyProperties(dto, defect);
        defect.setStatus(DefectStatusEnum.NEW.getCode());
        defect.setReporterId(UserContext.getUserId());
        defect.setReopenCount(0);
        save(defect);
        saveLog(defect.getId(), DefectActionEnum.CREATE.getCode(), null, defect.getStatus(), "创建缺陷");
        return toVO(defect);
    }

    @Override
    public DefectVO update(Long id, DefectDTO dto) {
        Defect defect = getById(id);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        // 仅编辑描述性字段，所属项目、状态、处理人不在此变更
        BeanUtil.copyProperties(dto, defect, "projectId");
        defect.setId(id);
        updateById(defect);
        saveLog(id, DefectActionEnum.UPDATE.getCode(), defect.getStatus(), defect.getStatus(), "编辑缺陷");
        return toVO(getById(id));
    }

    @Override
    public void delete(Long id) {
        Defect defect = getById(id);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        removeById(id);
    }

    @Override
    public DefectVO get(Long id) {
        Defect defect = getById(id);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        return toVO(defect);
    }

    @Override
    public PageResult<DefectVO> list(long current, long size, Long projectId, String status,
                                     String type, String priority, Long assigneeId, String keyword) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            projectService.assertMember(projectId);
            wrapper.eq(Defect::getProjectId, projectId);
        } else if (!projectService.isAdmin()) {
            // 非管理员且未指定项目时，仅返回有权限项目的缺陷
            Set<Long> ids = projectService.getAccessibleProjectIds();
            if (ids.isEmpty()) {
                return PageResult.of(new Page<Defect>(current, size), d -> toVO(d));
            }
            wrapper.in(Defect::getProjectId, ids);
        }
        wrapper.eq(StrUtil.isNotBlank(status), Defect::getStatus, status)
                .eq(StrUtil.isNotBlank(type), Defect::getType, type)
                .eq(StrUtil.isNotBlank(priority), Defect::getPriority, priority)
                .eq(assigneeId != null, Defect::getAssigneeId, assigneeId)
                .like(StrUtil.isNotBlank(keyword), Defect::getTitle, keyword)
                .orderByDesc(Defect::getId);
        Page<Defect> defectPage = page(new Page<>(current, size), wrapper);
        return PageResult.of(defectPage, d -> toVO(d));
    }

    @Override
    public DefectVO transition(Long id, DefectTransitionDTO dto) {
        Defect defect = getById(id);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        applyTransition(defect, dto);
        return toVO(getById(id));
    }

    @Override
    public DefectCommentVO addComment(Long id, DefectCommentDTO dto) {
        Defect defect = getById(id);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());

        DefectComment comment = new DefectComment();
        comment.setDefectId(id);
        comment.setUserId(UserContext.getUserId());
        comment.setContent(dto.getContent());
        commentMapper.insert(comment);
        saveLog(id, DefectActionEnum.COMMENT.getCode(), defect.getStatus(), defect.getStatus(), "添加评论");
        return toCommentVO(comment, batchUsers(Collections.singleton(comment.getUserId())));
    }

    @Override
    public List<DefectCommentVO> listComments(Long id) {
        Defect defect = getById(id);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        List<DefectComment> comments = commentMapper.selectList(new LambdaQueryWrapper<DefectComment>()
                .eq(DefectComment::getDefectId, id)
                .orderByAsc(DefectComment::getId));
        Map<Long, User> userMap = batchUsers(comments.stream()
                .map(DefectComment::getUserId).collect(Collectors.toSet()));
        return comments.stream().map(c -> toCommentVO(c, userMap)).collect(Collectors.toList());
    }

    @Override
    public List<DefectLogVO> listLogs(Long id) {
        Defect defect = getById(id);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        List<DefectLog> logs = logMapper.selectList(new LambdaQueryWrapper<DefectLog>()
                .eq(DefectLog::getDefectId, id)
                .orderByDesc(DefectLog::getId));
        Map<Long, User> userMap = batchUsers(logs.stream()
                .map(DefectLog::getOperatorId).collect(Collectors.toSet()));
        return logs.stream().map(l -> {
            DefectLogVO vo = DefectLogVO.from(l);
            User u = userMap.get(l.getOperatorId());
            vo.setOperatorName(u == null ? null : StrUtil.blankToDefault(u.getNickname(), u.getUsername()));
            return vo;
        }).collect(Collectors.toList());
    }

    // ---- 状态机 ----

    private void applyTransition(Defect defect, DefectTransitionDTO dto) {
        String action = dto.getAction().toUpperCase();
        String current = defect.getStatus();
        LambdaUpdateWrapper<Defect> uw = new LambdaUpdateWrapper<>();
        uw.eq(Defect::getId, defect.getId());
        String newStatus;

        switch (action) {
            case "ASSIGN" -> {
                requireStatus(current, DefectStatusEnum.NEW.getCode());
                if (dto.getAssigneeId() == null) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分配操作需指定处理人");
                }
                newStatus = DefectStatusEnum.ASSIGNED.getCode();
                uw.set(Defect::getStatus, newStatus).set(Defect::getAssigneeId, dto.getAssigneeId());
            }
            case "START" -> {
                requireStatus(current, DefectStatusEnum.ASSIGNED.getCode());
                newStatus = DefectStatusEnum.PROCESSING.getCode();
                uw.set(Defect::getStatus, newStatus);
            }
            case "RESOLVE" -> {
                requireStatus(current, DefectStatusEnum.PROCESSING.getCode());
                if (StrUtil.isBlank(dto.getSolution())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "标记解决需填写解决方案");
                }
                newStatus = DefectStatusEnum.PENDING_RETEST.getCode();
                uw.set(Defect::getStatus, newStatus)
                        .set(Defect::getSolution, dto.getSolution())
                        .set(Defect::getResolvedTime, LocalDateTime.now());
            }
            case "CLOSE" -> {
                requireStatus(current, DefectStatusEnum.PENDING_RETEST.getCode());
                newStatus = DefectStatusEnum.CLOSED.getCode();
                uw.set(Defect::getStatus, newStatus).set(Defect::getClosedTime, LocalDateTime.now());
            }
            case "REJECT" -> {
                requireStatus(current, DefectStatusEnum.PENDING_RETEST.getCode());
                newStatus = DefectStatusEnum.REJECTED.getCode();
                uw.set(Defect::getStatus, newStatus);
            }
            case "REOPEN" -> {
                requireStatus(current, DefectStatusEnum.REJECTED.getCode());
                newStatus = DefectStatusEnum.PROCESSING.getCode();
                int reopen = defect.getReopenCount() == null ? 0 : defect.getReopenCount();
                uw.set(Defect::getStatus, newStatus)
                        .set(Defect::getReopenCount, reopen + 1)
                        .set(Defect::getResolvedTime, null)
                        .set(Defect::getClosedTime, null);
            }
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法操作类型: " + action);
        }

        update(uw);
        saveLog(defect.getId(), action, current, newStatus, dto.getRemark());
    }

    private void requireStatus(String current, String expected) {
        if (!expected.equals(current)) {
            throw new BusinessException(ResultCode.DEFECT_STATUS_ILLEGAL.getCode(),
                    "当前状态【" + DefectStatusEnum.descOf(current) + "】不允许执行该操作");
        }
    }

    private void saveLog(Long defectId, String action, String fromStatus, String toStatus, String remark) {
        DefectLog log = new DefectLog();
        log.setDefectId(defectId);
        log.setOperatorId(UserContext.getUserId());
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        logMapper.insert(log);
    }

    // ---- 私有 ----

    private DefectVO toVO(Defect d) {
        DefectVO vo = DefectVO.from(d);
        if (d.getReporterId() != null) {
            User u = userMapper.selectById(d.getReporterId());
            vo.setReporterName(u == null ? null : StrUtil.blankToDefault(u.getNickname(), u.getUsername()));
        }
        if (d.getAssigneeId() != null) {
            User u = userMapper.selectById(d.getAssigneeId());
            vo.setAssigneeName(u == null ? null : StrUtil.blankToDefault(u.getNickname(), u.getUsername()));
        }
        return vo;
    }

    private DefectCommentVO toCommentVO(DefectComment c, Map<Long, User> userMap) {
        DefectCommentVO vo = new DefectCommentVO();
        vo.setId(c.getId());
        vo.setDefectId(c.getDefectId());
        vo.setUserId(c.getUserId());
        vo.setContent(c.getContent());
        vo.setCreateTime(c.getCreateTime());
        User u = userMap.get(c.getUserId());
        if (u != null) {
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
        }
        return vo;
    }

    private Map<Long, User> batchUsers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }
}
