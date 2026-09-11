package com.defect.platform.vo;

import lombok.Data;

import java.util.List;

/**
 * RAG 根因/方案推荐结果（Top3）
 */
@Data
public class AiRecommendVO {

    /** 实际用于检索的文本 */
    private String query;

    /** 检索方式：VECTOR / FULLTEXT / KEYWORD / NONE */
    private String retrievalMode;

    /** 检索到的候选知识总数 */
    private Integer candidateCount;

    /** Top3 推荐知识 */
    private List<AiRecommendItemVO> recommendations;

    /** AI 根因推测 */
    private String rootCauseGuess;

    /** AI 建议解决方案 */
    private String suggestedSolution;

    /** AI 综合建议 */
    private String summary;

    /** 结果来源：LLM（大模型）/ RULE（本地规则降级）/ NONE（无候选） */
    private String source;
}
