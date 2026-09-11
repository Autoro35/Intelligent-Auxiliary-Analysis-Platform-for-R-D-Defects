package com.defect.platform.service;

import com.defect.platform.entity.Knowledge;
import com.defect.platform.vo.RetrievalResultVO;

/**
 * 知识检索与向量索引服务
 * <p>检索采用三级降级策略，保证任一外部依赖缺失时 RAG 能力仍可用：</p>
 * <ol>
 *   <li>VECTOR —— Chroma 向量相似检索（语义匹配，效果最好）</li>
 *   <li>FULLTEXT —— t_knowledge 的 ngram 全文索引（中文免分词，MySQL 原生）</li>
 *   <li>KEYWORD —— 关键词模糊匹配（兜底，保证总能拿到候选）</li>
 * </ol>
 */
public interface KnowledgeRetrievalService {

    /**
     * 检索相关知识
     *
     * @param query 检索文本（通常是缺陷标题 + 描述）
     * @param topN  返回条数上限
     * @return 命中列表及实际生效的检索方式
     */
    RetrievalResultVO search(String query, int topN);

    /**
     * 写入/更新向量索引（尽力而为，失败仅记录日志，不影响主流程）
     */
    void index(Knowledge knowledge);

    /**
     * 删除向量索引（尽力而为）
     */
    void remove(Long knowledgeId);

    /**
     * 全量重建向量索引
     *
     * @return 成功写入的条数
     */
    int rebuildAll();

    /**
     * 向量库是否可用
     */
    boolean vectorAvailable();

    /**
     * 向量库中的向量数量，-1 表示不可用
     */
    long vectorCount();
}
