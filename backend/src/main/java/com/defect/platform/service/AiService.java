package com.defect.platform.service;

import com.defect.platform.dto.AiClassifyDTO;
import com.defect.platform.dto.AiCompleteDTO;
import com.defect.platform.dto.AiRecommendDTO;
import com.defect.platform.vo.AiClassifyVO;
import com.defect.platform.vo.AiCompleteVO;
import com.defect.platform.vo.AiRecommendVO;
import com.defect.platform.vo.AiStatusVO;

/**
 * AI 辅助能力服务
 * <p>三项能力均遵循「大模型优先、本地规则兜底」：DeepSeek 未配置或调用失败时自动降级，
 * 接口永远返回可用结果，响应中的 source 字段标明结果来源（LLM / RULE）。</p>
 */
public interface AiService {

    /** 自动分类与优先级判定 */
    AiClassifyVO classify(AiClassifyDTO dto);

    /** RAG 根因/方案推荐 Top3 */
    AiRecommendVO recommend(AiRecommendDTO dto);

    /** 缺陷描述补全提示 */
    AiCompleteVO completeDescription(AiCompleteDTO dto);

    /** AI 能力状态自检（部署验收用） */
    AiStatusVO status();

    /** 全量重建向量索引，返回成功条数 */
    int rebuildIndex();
}
