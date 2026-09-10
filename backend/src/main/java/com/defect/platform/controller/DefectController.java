package com.defect.platform.controller;

import com.defect.platform.common.PageResult;
import com.defect.platform.common.Result;
import com.defect.platform.dto.DefectCommentDTO;
import com.defect.platform.dto.DefectDTO;
import com.defect.platform.dto.DefectTransitionDTO;
import com.defect.platform.service.DefectService;
import com.defect.platform.vo.DefectCommentVO;
import com.defect.platform.vo.DefectLogVO;
import com.defect.platform.vo.DefectVO;
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
 * 缺陷接口：CRUD、生命周期流转、评论、操作日志
 */
@RestController
@RequestMapping("/api/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectService defectService;

    @PostMapping
    public Result<DefectVO> create(@Valid @RequestBody DefectDTO dto) {
        return Result.success("创建成功", defectService.create(dto));
    }

    @GetMapping
    public Result<PageResult<DefectVO>> list(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Long projectId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String priority,
                                             @RequestParam(required = false) Long assigneeId,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(defectService.list(current, size, projectId, status, type, priority, assigneeId, keyword));
    }

    @GetMapping("/{id}")
    public Result<DefectVO> get(@PathVariable Long id) {
        return Result.success(defectService.get(id));
    }

    @PutMapping("/{id}")
    public Result<DefectVO> update(@PathVariable Long id, @Valid @RequestBody DefectDTO dto) {
        return Result.success("更新成功", defectService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        defectService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/transition")
    public Result<DefectVO> transition(@PathVariable Long id, @Valid @RequestBody DefectTransitionDTO dto) {
        return Result.success("操作成功", defectService.transition(id, dto));
    }

    @PostMapping("/{id}/comments")
    public Result<DefectCommentVO> addComment(@PathVariable Long id, @Valid @RequestBody DefectCommentDTO dto) {
        return Result.success("评论成功", defectService.addComment(id, dto));
    }

    @GetMapping("/{id}/comments")
    public Result<List<DefectCommentVO>> listComments(@PathVariable Long id) {
        return Result.success(defectService.listComments(id));
    }

    @GetMapping("/{id}/logs")
    public Result<List<DefectLogVO>> listLogs(@PathVariable Long id) {
        return Result.success(defectService.listLogs(id));
    }
}
