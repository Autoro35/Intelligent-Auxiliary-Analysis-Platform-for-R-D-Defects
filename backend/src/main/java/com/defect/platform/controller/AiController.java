package com.defect.platform.controller;

import com.defect.platform.common.Result;
import com.defect.platform.common.annotation.RequireRole;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.dto.AiClassifyDTO;
import com.defect.platform.dto.AiCompleteDTO;
import com.defect.platform.dto.AiRecommendDTO;
import com.defect.platform.service.AiService;
import com.defect.platform.vo.AiClassifyVO;
import com.defect.platform.vo.AiCompleteVO;
import com.defect.platform.vo.AiRecommendVO;
import com.defect.platform.vo.AiStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 辅助能力接口
 * <p>权限划分：分类判定与描述补全服务于缺陷提单，仅管理员/测试/开发可用；
 * RAG 知识推荐本质是知识库只读检索，对所有登录用户开放（含访客）；
 * 状态自检与索引重建涉及基础设施，仅管理员可用。</p>
 * <p>三项能力均不写库、不改动缺陷状态，因此不参与项目隔离；但推荐结果来自全局知识库，
 * 与知识库的可见范围保持一致。</p>
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/classify")
    @RequireRole({RoleEnum.ADMIN, RoleEnum.TESTER, RoleEnum.DEVELOPER})
    public Result<AiClassifyVO> classify(@RequestBody AiClassifyDTO dto) {
        return Result.success(aiService.classify(dto));
    }

    @PostMapping("/recommend")
    public Result<AiRecommendVO> recommend(@RequestBody AiRecommendDTO dto) {
        return Result.success(aiService.recommend(dto));
    }

    @PostMapping("/complete-description")
    @RequireRole({RoleEnum.ADMIN, RoleEnum.TESTER, RoleEnum.DEVELOPER})
    public Result<AiCompleteVO> completeDescription(@RequestBody AiCompleteDTO dto) {
        return Result.success(aiService.completeDescription(dto));
    }

    @GetMapping("/status")
    @RequireRole({RoleEnum.ADMIN})
    public Result<AiStatusVO> status() {
        return Result.success(aiService.status());
    }

    @PostMapping("/index/rebuild")
    @RequireRole({RoleEnum.ADMIN})
    public Result<Integer> rebuildIndex() {
        return Result.success("向量索引重建完成", aiService.rebuildIndex());
    }
}
