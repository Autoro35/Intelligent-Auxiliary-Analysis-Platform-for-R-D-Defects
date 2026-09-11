package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.constant.DefectTypeEnum;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.entity.Knowledge;
import com.defect.platform.mapper.KnowledgeMapper;
import com.defect.platform.service.KnowledgeRetrievalService;
import com.defect.platform.utils.AiRuleEngine;
import com.defect.platform.utils.ChromaClient;
import com.defect.platform.vo.KnowledgeSearchHitVO;
import com.defect.platform.vo.RetrievalHitVO;
import com.defect.platform.vo.RetrievalResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识检索与向量索引服务实现
 * <p>检索链路：向量 → 全文索引 → 关键词模糊，逐级降级，任一级命中即返回；</p>
 * <p>索引写入为「尽力而为」：向量库不可用只记日志，绝不阻断知识库的增删改主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    /** 关键词模糊匹配最多使用几个关键词，避免 OR 条件过长 */
    private static final int MAX_KEYWORDS = 4;

    private final KnowledgeMapper knowledgeMapper;
    private final ChromaClient chromaClient;
    private final AiRuleEngine ruleEngine;

    @Override
    public RetrievalResultVO search(String query, int topN) {
        if (StrUtil.isBlank(query) || topN <= 0) {
            return new RetrievalResultVO("NONE", Collections.emptyList());
        }

        // 一级：向量检索（语义相似，效果最好）
        if (chromaClient.isReachable()) {
            // 多取一倍候选，回表过滤掉已逻辑删除的记录后仍能凑够 topN
            List<RetrievalHitVO> vectorHits = searchVector(query, topN);
            if (!vectorHits.isEmpty()) {
                return new RetrievalResultVO("VECTOR", vectorHits);
            }
        }

        // 二级：MySQL ngram 全文索引
        List<RetrievalHitVO> fulltextHits = searchFulltext(query, topN);
        if (!fulltextHits.isEmpty()) {
            return new RetrievalResultVO("FULLTEXT", fulltextHits);
        }

        // 三级：关键词模糊匹配（兜底）
        List<RetrievalHitVO> keywordHits = searchByKeyword(query, topN);
        return new RetrievalResultVO(keywordHits.isEmpty() ? "NONE" : "KEYWORD", keywordHits);
    }

    @Override
    public void index(Knowledge knowledge) {
        if (knowledge == null || knowledge.getId() == null) {
            return;
        }
        try {
            chromaClient.upsert(knowledge.getId(), buildDocument(knowledge), buildMetadata(knowledge));
        } catch (Exception e) {
            // 向量索引是增强能力，失败不能影响知识库主流程
            log.warn("写入向量索引失败(已忽略): knowledgeId={}, err={}", knowledge.getId(), e.getMessage());
        }
    }

    @Override
    public void remove(Long knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        try {
            chromaClient.delete(knowledgeId);
        } catch (Exception e) {
            log.warn("删除向量索引失败(已忽略): knowledgeId={}, err={}", knowledgeId, e.getMessage());
        }
    }

    @Override
    public int rebuildAll() {
        if (!chromaClient.isReachable()) {
            throw new BusinessException(ResultCode.VECTOR_STORE_UNAVAILABLE);
        }
        List<Knowledge> all = knowledgeMapper.selectList(null);
        int success = 0;
        for (Knowledge knowledge : all) {
            if (chromaClient.upsert(knowledge.getId(), buildDocument(knowledge), buildMetadata(knowledge))) {
                success++;
            }
        }
        log.info("向量索引重建完成: 知识总数={}, 成功={}", all.size(), success);
        return success;
    }

    @Override
    public boolean vectorAvailable() {
        return chromaClient.isReachable();
    }

    @Override
    public long vectorCount() {
        return chromaClient.count();
    }

    // ---- 三级检索实现 ----

    /**
     * 一级：向量检索，Chroma 返回的距离转为 0-1 相似度；回表过滤已删除记录
     */
    private List<RetrievalHitVO> searchVector(String query, int topN) {
        List<ChromaClient.Hit> hits = chromaClient.query(query, topN * 2);
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }
        // 按业务 ID 批量回表，既保证数据最新，也过滤逻辑删除的记录
        Map<Long, Knowledge> loaded = loadByIds(hits.stream()
                .map(ChromaClient.Hit::getKnowledgeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        List<RetrievalHitVO> result = new ArrayList<>(topN);
        for (ChromaClient.Hit hit : hits) {
            if (result.size() >= topN) {
                break;
            }
            Knowledge knowledge = hit.getKnowledgeId() == null ? null : loaded.get(hit.getKnowledgeId());
            if (knowledge == null) {
                continue;
            }
            RetrievalHitVO vo = toHit(knowledge, "VECTOR");
            vo.setScore(round(hit.getScore()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 二级：全文索引检索，MATCH 原始分按批次最大值归一化到 0-1
     */
    private List<RetrievalHitVO> searchFulltext(String query, int topN) {
        List<KnowledgeSearchHitVO> rows;
        try {
            rows = knowledgeMapper.searchFulltext(query, topN);
        } catch (Exception e) {
            log.warn("全文索引检索失败，降级为关键词模糊匹配: {}", e.getMessage());
            return Collections.emptyList();
        }
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        double max = rows.stream()
                .mapToDouble(r -> r.getScore() == null ? 0D : r.getScore())
                .max().orElse(0D);

        List<RetrievalHitVO> result = new ArrayList<>(rows.size());
        for (KnowledgeSearchHitVO row : rows) {
            RetrievalHitVO vo = new RetrievalHitVO();
            vo.setKnowledgeId(row.getId());
            vo.setTitle(row.getTitle());
            vo.setType(row.getType());
            vo.setTypeDesc(DefectTypeEnum.descOf(row.getType()));
            vo.setRootCause(row.getRootCause());
            vo.setSolution(row.getSolution());
            vo.setTags(row.getTags());
            vo.setTagList(splitTags(row.getTags()));
            vo.setScore(max <= 0 ? null : round(row.getScore() == null ? 0D : row.getScore() / max));
            vo.setMatchedBy("FULLTEXT");
            result.add(vo);
        }
        return result;
    }

    /**
     * 三级：关键词模糊匹配，按命中顺序给出递减的排名分
     */
    private List<RetrievalHitVO> searchByKeyword(String query, int topN) {
        List<String> keywords = ruleEngine.extractKeywords(query, MAX_KEYWORDS);
        if (keywords.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> {
            boolean first = true;
            for (String keyword : keywords) {
                if (!first) {
                    w.or();
                }
                w.like(Knowledge::getTitle, keyword)
                        .or().like(Knowledge::getRootCause, keyword)
                        .or().like(Knowledge::getSolution, keyword);
                first = false;
            }
        });
        Page<Knowledge> page = knowledgeMapper.selectPage(new Page<>(1, topN), wrapper);

        List<RetrievalHitVO> result = new ArrayList<>();
        List<Knowledge> records = page.getRecords();
        for (int i = 0; i < records.size(); i++) {
            RetrievalHitVO vo = toHit(records.get(i), "KEYWORD");
            // 模糊匹配无真实相似度，按排名给出递减分值，仅用于前端排序展示
            vo.setScore(round((double) (topN - i) / topN));
            result.add(vo);
        }
        return result;
    }

    // ---- 工具 ----

    private Map<Long, Knowledge> loadByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Knowledge> map = new LinkedHashMap<>(ids.size());
        for (Knowledge knowledge : knowledgeMapper.selectBatchIds(ids)) {
            map.put(knowledge.getId(), knowledge);
        }
        return map;
    }

    private RetrievalHitVO toHit(Knowledge knowledge, String matchedBy) {
        RetrievalHitVO vo = new RetrievalHitVO();
        vo.setKnowledgeId(knowledge.getId());
        vo.setTitle(knowledge.getTitle());
        vo.setType(knowledge.getType());
        vo.setTypeDesc(DefectTypeEnum.descOf(knowledge.getType()));
        vo.setRootCause(knowledge.getRootCause());
        vo.setSolution(knowledge.getSolution());
        vo.setTags(knowledge.getTags());
        vo.setTagList(splitTags(knowledge.getTags()));
        vo.setMatchedBy(matchedBy);
        return vo;
    }

    private List<String> splitTags(String tags) {
        if (StrUtil.isBlank(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    /** 保留 3 位小数，避免前端展示一长串浮点尾数 */
    private Double round(Double value) {
        return value == null ? null : Math.round(value * 1000.0) / 1000.0;
    }

    /** 送入向量库的文档正文：标题 + 根因 + 方案，与全文索引覆盖的字段保持一致 */
    private String buildDocument(Knowledge knowledge) {
        return StrUtil.blankToDefault(knowledge.getTitle(), "")
                + "\n" + StrUtil.blankToDefault(knowledge.getRootCause(), "")
                + "\n" + StrUtil.blankToDefault(knowledge.getSolution(), "");
    }

    private Map<String, Object> buildMetadata(Knowledge knowledge) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("knowledge_id", knowledge.getId());
        metadata.put("title", StrUtil.blankToDefault(knowledge.getTitle(), ""));
        if (StrUtil.isNotBlank(knowledge.getType())) {
            metadata.put("type", knowledge.getType());
        }
        return metadata;
    }
}
