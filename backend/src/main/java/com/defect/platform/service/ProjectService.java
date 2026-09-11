package com.defect.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.defect.platform.common.PageResult;
import com.defect.platform.dto.ProjectDTO;
import com.defect.platform.dto.ProjectMemberDTO;
import com.defect.platform.entity.Project;
import com.defect.platform.vo.ProjectMemberVO;
import com.defect.platform.vo.ProjectVO;

import java.util.List;
import java.util.Set;

/**
 * 项目服务：CRUD、成员管理、多项目隔离
 */
public interface ProjectService extends IService<Project> {

    ProjectVO create(ProjectDTO dto);

    ProjectVO update(Long id, ProjectDTO dto);

    void delete(Long id);

    ProjectVO get(Long id);

    PageResult<ProjectVO> list(long current, long size, String keyword);

    void addMember(Long projectId, ProjectMemberDTO dto);

    void removeMember(Long projectId, Long userId);

    List<ProjectMemberVO> listMembers(Long projectId);

    // ---- 权限辅助（供缺陷/附件模块复用） ----

    /** 校验当前用户为项目成员或管理员，否则抛异常 */
    void assertMember(Long projectId);

    /** 校验当前用户为项目负责人或管理员，否则抛异常 */
    void assertOwner(Long projectId);

    /** 当前用户可见的项目 ID 集合 */
    Set<Long> getAccessibleProjectIds();

    /** 当前用户是否管理员 */
    boolean isAdmin();
}
