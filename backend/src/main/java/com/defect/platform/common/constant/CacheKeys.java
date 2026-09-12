package com.defect.platform.common.constant;

/**
 * 缓存键定义
 * <p>统一前缀便于按业务域批量失效，避免散落在各处的字符串字面量拼错。</p>
 * <p><b>重要</b>：统计类键必须带用户维度。统计结果受多项目隔离影响，
 * 不同用户可见的项目集合不同，若共用同一个键会把 A 用户的数据串给 B 用户。</p>
 */
public final class CacheKeys {

    /** 统计模块前缀，缺陷/项目变动时整体失效 */
    public static final String STATS_PREFIX = "defect:stats:";

    /** 知识库模块前缀 */
    public static final String KNOWLEDGE_PREFIX = "defect:knowledge:";

    private CacheKeys() {
    }

    public static String statsOverview(Long userId) {
        return STATS_PREFIX + "overview:" + userId;
    }

    public static String statsDistribution(Long userId) {
        return STATS_PREFIX + "distribution:" + userId;
    }

    public static String statsTrend(Long userId, int days) {
        return STATS_PREFIX + "trend:" + userId + ":" + days;
    }

    public static String statsWorkload(Long userId) {
        return STATS_PREFIX + "workload:" + userId;
    }

    /** 知识库标签云为全局共享数据，无需用户维度 */
    public static String knowledgeTags() {
        return KNOWLEDGE_PREFIX + "tags";
    }
}
