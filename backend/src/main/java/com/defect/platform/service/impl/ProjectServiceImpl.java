package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defect.platform.common.PageResult;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.constant.ProjectRoleEnum;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.dto.ProjectDTO;
import com.defect.platform.dto.ProjectMemberDTO;
import com.defect.platform.entity.Project;
import com.defect.platform.entity.ProjectMember;
import com.defect.platform.entity.User;
import com.defect.platform.mapper.ProjectMapper;
import com.defect.platform.mapper.ProjectMemberMapper;
import com.defect.platform.mapper.UserMapper;
import com.defect.platform.service.ProjectService;
import com.defect.platform.vo.ProjectMemberVO;
import com.defect.platform.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目服务实现
 * <p>多项目隔离：非管理员仅能看到自己负责或加入的项目；成员操作需负责人或管理员权限</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;

    @Override
    public ProjectVO create(ProjectDTO dto) {
        // 项目编码唯一性
        long exists = count(new LambdaQueryWrapper<Project>().eq(Project::getCode, dto.getCode()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.PROJECT_CODE_EXISTS);
        }
        Long currentUserId = UserContext.getUserId();
        Long ownerId = dto.getOwnerId() != null ? dto.getOwnerId() : currentUserId;
        if (userMapper.selectById(ownerId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Project project = new Project();
        project.setName(dto.getName());
        project.setCode(dto.getCode());
        project.setDescription(dto.getDescription());
        project.setOwnerId(ownerId);
        project.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        save(project);

        // 负责人自动成为项目成员
        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProjectId(project.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setRole(ProjectRoleEnum.OWNER.getCode());
        projectMemberMapper.insert(ownerMember);

        log.info("创建项目: id={}, code={}, owner={}", project.getId(), project.getCode(), ownerId);
        return toVO(project);
    }

    @Override
    public ProjectVO update(Long id, ProjectDTO dto) {
        Project project = getById(id);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        assertOwner(id);

        // 编码唯一性（排除自身）
        long exists = count(new LambdaQueryWrapper<Project>()
                .eq(Project::getCode, dto.getCode()).ne(Project::getId, id));
        if (exists > 0) {
            throw new BusinessException(ResultCode.PROJECT_CODE_EXISTS);
        }

        project.setName(dto.getName());
        project.setCode(dto.getCode());
        project.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            project.setStatus(dto.getStatus());
        }
        if (dto.getOwnerId() != null) {
            if (userMapper.selectById(dto.getOwnerId()) == null) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }
            project.setOwnerId(dto.getOwnerId());
        }
        updateById(project);
        return toVO(getById(id));
    }

    @Override
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        assertOwner(id);
        removeById(id);
    }

    @Override
    public ProjectVO get(Long id) {
        Project project = getById(id);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        assertMember(id);
        return toVO(project);
    }

    @Override
    public PageResult<ProjectVO> list(long current, long size, String keyword) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin()) {
            Set<Long> ids = getAccessibleProjectIds();
            if (ids.isEmpty()) {
                return PageResult.of(new Page<Project>(current, size), p -> toVO(p));
            }
            wrapper.in(Project::getId, ids);
        }
        wrapper.like(StrUtil.isNotBlank(keyword), Project::getName, keyword)
                .orderByDesc(Project::getId);
        Page<Project> projectPage = page(new Page<>(current, size), wrapper);
        return PageResult.of(projectPage, p -> toVO(p));
    }

    @Override
    public void addMember(Long projectId, ProjectMemberDTO dto) {
        if (getById(projectId) == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        assertOwner(projectId);
        if (userMapper.selectById(dto.getUserId()) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        String role = dto.getRole().toUpperCase();
        if (!isValidProjectRole(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法的项目内角色");
        }
        long exists = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, dto.getUserId()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.PROJECT_MEMBER_EXISTS);
        }
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(dto.getUserId());
        member.setRole(role);
        projectMemberMapper.insert(member);
    }

    @Override
    public void removeMember(Long projectId, Long userId) {
        if (getById(projectId) == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        assertOwner(projectId);
        projectMemberMapper.delete(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    @Override
    public List<ProjectMemberVO> listMembers(Long projectId) {
        if (getById(projectId) == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        assertMember(projectId);
        List<ProjectMember> members = projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getId));
        Map<Long, User> userMap = batchUsers(members.stream()
                .map(ProjectMember::getUserId).collect(Collectors.toSet()));
        return members.stream().map(m -> toMemberVO(m, userMap)).collect(Collectors.toList());
    }

    // ---- 权限辅助 ----

    @Override
    public boolean isAdmin() {
        return RoleEnum.ADMIN.getCode().equals(UserContext.getRole());
    }

    @Override
    public void assertMember(Long projectId) {
        // 项目存在性优先于管理员豁免：管理员同样不能对不存在的项目操作，避免产生孤儿数据
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (isAdmin()) {
            return;
        }
        Long userId = UserContext.getUserId();
        if (project.getOwnerId() != null && project.getOwnerId().equals(userId)) {
            return;
        }
        long member = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (member == 0) {
            throw new BusinessException(ResultCode.PROJECT_NOT_MEMBER);
        }
    }

    @Override
    public void assertOwner(Long projectId) {
        if (isAdmin()) {
            return;
        }
        Project project = getById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (project.getOwnerId() == null || !project.getOwnerId().equals(UserContext.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    @Override
    public Set<Long> getAccessibleProjectIds() {
        Long userId = UserContext.getUserId();
        Set<Long> ids = new HashSet<>();
        // 我负责的项目
        baseMapper.selectList(new LambdaQueryWrapper<Project>().eq(Project::getOwnerId, userId))
                .forEach(p -> ids.add(p.getId()));
        // 我加入的项目
        projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getUserId, userId))
                .forEach(m -> ids.add(m.getProjectId()));
        return ids;
    }

    // ---- 私有 ----

    private ProjectVO toVO(Project p) {
        ProjectVO vo = new ProjectVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setDescription(p.getDescription());
        vo.setStatus(p.getStatus());
        vo.setOwnerId(p.getOwnerId());
        if (p.getOwnerId() != null) {
            User owner = userMapper.selectById(p.getOwnerId());
            vo.setOwnerName(owner == null ? null : StrUtil.blankToDefault(owner.getNickname(), owner.getUsername()));
        }
        vo.setMemberCount(projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, p.getId())));
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private ProjectMemberVO toMemberVO(ProjectMember m, Map<Long, User> userMap) {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setUserId(m.getUserId());
        User u = userMap.get(m.getUserId());
        vo.setUsername(u == null ? null : u.getUsername());
        vo.setNickname(u == null ? null : u.getNickname());
        vo.setRole(m.getRole());
        vo.setRoleDesc(ProjectRoleEnum.descOf(m.getRole()));
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }

    private Map<Long, User> batchUsers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private boolean isValidProjectRole(String role) {
        for (ProjectRoleEnum r : ProjectRoleEnum.values()) {
            if (r.getCode().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
