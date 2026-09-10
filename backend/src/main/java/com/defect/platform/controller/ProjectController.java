package com.defect.platform.controller;

import com.defect.platform.common.PageResult;
import com.defect.platform.common.Result;
import com.defect.platform.dto.ProjectDTO;
import com.defect.platform.dto.ProjectMemberDTO;
import com.defect.platform.service.ProjectService;
import com.defect.platform.vo.ProjectMemberVO;
import com.defect.platform.vo.ProjectVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目接口：CRUD 与成员管理
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public Result<ProjectVO> create(@Valid @RequestBody ProjectDTO dto) {
        return Result.success("创建成功", projectService.create(dto));
    }

    @GetMapping
    public Result<PageResult<ProjectVO>> list(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(projectService.list(current, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<ProjectVO> get(@PathVariable Long id) {
        return Result.success(projectService.get(id));
    }

    @PutMapping("/{id}")
    public Result<ProjectVO> update(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto) {
        return Result.success("更新成功", projectService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/members")
    public Result<List<ProjectMemberVO>> listMembers(@PathVariable Long id) {
        return Result.success(projectService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public Result<Void> addMember(@PathVariable Long id, @Valid @RequestBody ProjectMemberDTO dto) {
        projectService.addMember(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return Result.success();
    }
}
