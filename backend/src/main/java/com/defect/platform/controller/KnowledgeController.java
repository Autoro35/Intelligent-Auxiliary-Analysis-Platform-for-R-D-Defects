package com.defect.platform.controller;

import com.defect.platform.common.PageResult;
import com.defect.platform.common.Result;
import com.defect.platform.common.annotation.RequireRole;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.dto.KnowledgeDTO;
import com.defect.platform.service.KnowledgeService;
import com.defect.platform.vo.KnowledgeVO;
import com.defect.platform.vo.TagVO;
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
 * 知识库接口：检索、标签云、CRUD、从缺陷沉淀
 * <p>查看/检索对所有登录用户开放；写操作仅管理员/测试/开发（访客只读）</p>
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public Result<PageResult<KnowledgeVO>> list(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String type,
                                                @RequestParam(required = false) String tag) {
        return Result.success(knowledgeService.list(current, size, keyword, type, tag));
    }

    @GetMapping("/tags")
    public Result<List<TagVO>> tags() {
        return Result.success(knowledgeService.tags());
    }

    @GetMapping("/{id}")
    public Result<KnowledgeVO> get(@PathVariable Long id) {
        return Result.success(knowledgeService.get(id));
    }

    @PostMapping
    @RequireRole({RoleEnum.ADMIN, RoleEnum.TESTER, RoleEnum.DEVELOPER})
    public Result<KnowledgeVO> create(@Valid @RequestBody KnowledgeDTO dto) {
        return Result.success("创建成功", knowledgeService.create(dto));
    }

    @PostMapping("/from-defect/{defectId}")
    @RequireRole({RoleEnum.ADMIN, RoleEnum.TESTER, RoleEnum.DEVELOPER})
    public Result<KnowledgeVO> precipitate(@PathVariable Long defectId,
                                           @RequestBody(required = false) KnowledgeDTO dto) {
        return Result.success("沉淀成功", knowledgeService.precipitate(defectId, dto == null ? new KnowledgeDTO() : dto));
    }

    @PutMapping("/{id}")
    @RequireRole({RoleEnum.ADMIN, RoleEnum.TESTER, RoleEnum.DEVELOPER})
    public Result<KnowledgeVO> update(@PathVariable Long id, @Valid @RequestBody KnowledgeDTO dto) {
        return Result.success("更新成功", knowledgeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleEnum.ADMIN, RoleEnum.TESTER, RoleEnum.DEVELOPER})
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }
}
