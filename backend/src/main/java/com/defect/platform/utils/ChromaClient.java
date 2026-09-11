package com.defect.platform.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.defect.platform.config.ChromaProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chroma 向量库客户端
 * <p>向量检索是「锦上添花」能力：本类所有方法<b>不抛异常</b>，任何失败都记录日志并返回
 * null / false / 空集合，由上层检索服务决定降级到 MySQL 全文索引。</p>
 * <p>兼容 Chroma 0.5+/1.x 的 /api/v2 路径与 0.4.x 的 /api/v1 路径（由 chroma.api-path 决定）。</p>
 */
@Slf4j
@Component
public class ChromaClient {

    /** 向量主键前缀，便于与业务 ID 互转 */
    private static final String ID_PREFIX = "knowledge-";

    /** 连通性探测结果缓存时长（毫秒），避免每次检索都打心跳 */
    private static final long REACHABLE_CACHE_MS = 30_000L;

    private final ChromaProperties properties;
    private final RestClient restClient;

    /** 缓存的集合 ID，首次解析成功后复用 */
    private volatile String cachedCollectionId;

    /** 连通性缓存 */
    private volatile boolean reachableCache;
    private volatile long reachableCheckedAt;

    public ChromaClient(ChromaProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeout());
        factory.setReadTimeout(properties.getTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /**
     * 心跳探测，带 30 秒缓存
     */
    public boolean isReachable() {
        long now = System.currentTimeMillis();
        if (now - reachableCheckedAt < REACHABLE_CACHE_MS) {
            return reachableCache;
        }
        boolean ok;
        try {
            restClient.get().uri(properties.getApiPath() + "/heartbeat").retrieve().body(String.class);
            ok = true;
        } catch (Exception e) {
            log.debug("Chroma 心跳失败: {}", e.getMessage());
            ok = false;
        }
        reachableCache = ok;
        reachableCheckedAt = now;
        return ok;
    }

    /**
     * 写入或更新一条向量（以业务 ID 为主键幂等覆盖）
     *
     * @return 成功返回 true；不可达或写入失败返回 false
     */
    public boolean upsert(Long knowledgeId, String document, Map<String, Object> metadata) {
        if (knowledgeId == null || StrUtil.isBlank(document)) {
            return false;
        }
        String collectionId = resolveCollectionId();
        if (collectionId == null) {
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ids", Collections.singletonList(ID_PREFIX + knowledgeId));
        body.put("documents", Collections.singletonList(document));
        if (metadata != null && !metadata.isEmpty()) {
            body.put("metadatas", Collections.singletonList(metadata));
        }
        try {
            restClient.post()
                    .uri(collectionUri(collectionId) + "/upsert")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return true;
        } catch (Exception e) {
            log.warn("Chroma 写入向量失败: knowledgeId={}, err={}", knowledgeId, e.getMessage());
            invalidateCache();
            return false;
        }
    }

    /**
     * 删除一条向量
     */
    public boolean delete(Long knowledgeId) {
        if (knowledgeId == null) {
            return false;
        }
        String collectionId = resolveCollectionId();
        if (collectionId == null) {
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ids", Collections.singletonList(ID_PREFIX + knowledgeId));
        try {
            restClient.post()
                    .uri(collectionUri(collectionId) + "/delete")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return true;
        } catch (Exception e) {
            log.warn("Chroma 删除向量失败: knowledgeId={}, err={}", knowledgeId, e.getMessage());
            invalidateCache();
            return false;
        }
    }

    /**
     * 文本相似检索（由 Chroma 服务端完成向量化）
     *
     * @param queryText 查询文本
     * @param nResults  返回条数
     * @return 不可达或失败时返回空集合（非 null）
     */
    public List<Hit> query(String queryText, int nResults) {
        if (StrUtil.isBlank(queryText)) {
            return Collections.emptyList();
        }
        String collectionId = resolveCollectionId();
        if (collectionId == null) {
            return Collections.emptyList();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query_texts", Collections.singletonList(queryText));
        body.put("n_results", nResults);
        body.put("include", List.of("documents", "metadatas", "distances"));
        try {
            String raw = restClient.post()
                    .uri(collectionUri(collectionId) + "/query")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseHits(raw);
        } catch (Exception e) {
            log.warn("Chroma 检索失败: {}", e.getMessage());
            invalidateCache();
            return Collections.emptyList();
        }
    }

    /**
     * 集合内向量总数，不可用时返回 -1
     */
    public long count() {
        String collectionId = resolveCollectionId();
        if (collectionId == null) {
            return -1L;
        }
        try {
            String raw = restClient.post()
                    .uri(collectionUri(collectionId) + "/count")
                    .retrieve()
                    .body(String.class);
            if (StrUtil.isBlank(raw)) {
                return -1L;
            }
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            log.debug("Chroma 计数失败: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 当前使用的集合名（用于状态展示）
     */
    public String collectionName() {
        return properties.getCollection();
    }

    // ---- 私有 ----

    /**
     * 解析集合 ID，首次调用时按 get_or_create 语义创建
     *
     * @return 解析失败返回 null（视为向量库不可用）
     */
    private String resolveCollectionId() {
        if (StrUtil.isNotBlank(cachedCollectionId)) {
            return cachedCollectionId;
        }
        if (!isReachable()) {
            return null;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", properties.getCollection());
            body.put("get_or_create", true);
            String raw = restClient.post()
                    .uri(collectionsPath())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JSONObject obj = JSONUtil.parseObj(raw);
            String id = obj.getStr("id");
            if (StrUtil.isBlank(id)) {
                log.warn("Chroma 集合创建响应缺少 id: {}", StrUtil.maxLength(raw, 200));
                return null;
            }
            cachedCollectionId = id;
            log.info("Chroma 集合就绪: name={}, id={}", properties.getCollection(), id);
            return id;
        } catch (Exception e) {
            log.warn("Chroma 集合解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 集合列表路径：v2 需要租户/数据库前缀，v1 不需要
     */
    private String collectionsPath() {
        String apiPath = properties.getApiPath();
        if (apiPath != null && apiPath.contains("/v2")) {
            return apiPath + "/tenants/" + properties.getTenant()
                    + "/databases/" + properties.getDatabase() + "/collections";
        }
        return apiPath + "/collections";
    }

    /**
     * 单个集合的操作路径前缀（add/upsert/query/delete/count）
     */
    private String collectionUri(String collectionId) {
        return collectionsPath() + "/" + collectionId;
    }

    /**
     * 失败后清空缓存，让下次调用重新探测与解析
     */
    private void invalidateCache() {
        cachedCollectionId = null;
        reachableCheckedAt = 0L;
    }

    /**
     * 解析 Chroma query 响应：ids/documents/metadatas/distances 均为二维数组
     */
    private List<Hit> parseHits(String raw) {
        if (StrUtil.isBlank(raw)) {
            return Collections.emptyList();
        }
        JSONObject root = JSONUtil.parseObj(raw);
        JSONArray ids = root.getJSONArray("ids");
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        JSONArray documents = root.getJSONArray("documents");
        JSONArray metadatas = root.getJSONArray("metadatas");
        JSONArray distances = root.getJSONArray("distances");

        // 批次维度（第 0 批即本次查询）
        JSONArray idRow = ids.getJSONArray(0);
        if (idRow == null || idRow.isEmpty()) {
            return Collections.emptyList();
        }
        JSONArray docRow = firstRow(documents);
        JSONArray metaRow = firstRow(metadatas);
        JSONArray distRow = firstRow(distances);

        List<Hit> hits = new ArrayList<>(idRow.size());
        for (int i = 0; i < idRow.size(); i++) {
            Hit hit = new Hit();
            hit.setId(idRow.getStr(i));
            hit.setKnowledgeId(parseKnowledgeId(hit.getId()));
            hit.setDocument(docRow == null || i >= docRow.size() ? null : docRow.getStr(i));
            JSONObject meta = metaRow == null || i >= metaRow.size() ? null : metaRow.getJSONObject(i);
            hit.setTitle(meta == null ? null : meta.getStr("title"));
            hit.setType(meta == null ? null : meta.getStr("type"));
            // Chroma 返回距离（越小越相似），转为 0-1 的相似度便于前端展示
            if (distRow != null && i < distRow.size()) {
                Double distance = distRow.getDouble(i);
                hit.setDistance(distance);
                hit.setScore(distance == null ? null : 1.0 / (1.0 + distance));
            }
            hits.add(hit);
        }
        return hits;
    }

    private JSONArray firstRow(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return null;
        }
        return array.getJSONArray(0);
    }

    private Long parseKnowledgeId(String vectorId) {
        if (StrUtil.isBlank(vectorId) || !vectorId.startsWith(ID_PREFIX)) {
            return null;
        }
        try {
            return Long.valueOf(vectorId.substring(ID_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 向量检索命中项（仅供上层检索服务使用）
     */
    @Data
    public static class Hit {

        /** 向量主键，形如 knowledge-1 */
        private String id;

        /** 业务知识 ID */
        private Long knowledgeId;

        /** 原始文档文本 */
        private String document;

        /** 元数据中的标题 */
        private String title;

        /** 元数据中的缺陷类型 */
        private String type;

        /** 距离（越小越相似） */
        private Double distance;

        /** 相似度（0-1，越大越相似） */
        private Double score;
    }
}
