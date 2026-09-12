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
    //
    // 两个方法遵循同一语义：先校验项目存在（不存在抛 PROJECT_NOT_FOUND），再判断权限。
    // 管理员豁免的只是「权限判断」这一步，不豁免存在性校验——
    // 否则管理员可以对不存在的项目操作，产生孤儿数据。
    // 调用方无需再自行做存在性校验。

    /** 校验当前用户为项目成员或管理员；项目不存在时抛 PROJECT_NOT_FOUND */
    void assertMember(Long projectId);

    /** 校验当前用户为项目负责人或管理员；项目不存在时抛 PROJECT_NOT_FOUND */
    void assertOwner(Long projectId);

    /** 当前用户可见的项目 ID 集合 */
    Set<Long> getAccessibleProjectIds();

    /** 当前用户是否管理员 */
    boolean isAdmin();
}
