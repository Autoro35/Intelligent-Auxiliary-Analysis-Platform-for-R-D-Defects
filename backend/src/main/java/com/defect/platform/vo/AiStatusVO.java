package com.defect.platform.vo;

import lombok.Data;

/**
 * AI 能力状态（用于部署自检与验收）
 */
@Data
public class AiStatusVO {

    /** 是否已配置 DeepSeek API Key */
    private Boolean deepseekConfigured;

    /** DeepSeek 是否连通（会发起一次极小真实调用） */
    private Boolean deepseekReachable;

    /** 当前使用的模型 */
    private String model;

    /** Chroma 向量库是否连通 */
    private Boolean chromaReachable;

    /** 向量库集合名 */
    private String chromaCollection;

    /** 向量库中的向量数量，-1 表示不可用 */
    private Long vectorCount;

    /** 知识库条目总数（作为索引覆盖率参照） */
    private Long knowledgeCount;

    /** 当前实际生效的检索方式：VECTOR / FULLTEXT */
    private String retrievalMode;
}
