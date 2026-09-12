package com.defect.platform.service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 缓存服务
 * <p><b>降级语义</b>：Redis 不可用（未部署 / 连不上 / 操作超时）时，所有方法都直接执行 loader 并返回结果，
 * 等同于无缓存，调用方无需感知、也不应因此抛错。项目的定位是「开箱即用」，
 * 不能因为少部署一个 Redis 就跑不起来。</p>
 * <p>缓存值统一序列化为 JSON 存储，因此存入的类型必须可被 Hutool 正常序列化/反序列化。</p>
 */
public interface CacheService {

    /**
     * 读缓存，未命中的话执行 loader 并把结果写回
     *
     * @param key  缓存键
     * @param ttl  过期时间
     * @param type 返回值类型
     * @param loader 回源逻辑
     */
    <T> T get(String key, Duration ttl, Class<T> type, Supplier<T> loader);

    /**
     * 读缓存（列表版本）
     *
     * @param elementType 列表元素类型
     */
    <T> List<T> getList(String key, Duration ttl, Class<T> elementType, Supplier<List<T>> loader);

    /**
     * 删除单个键
     */
    void evict(String key);

    /**
     * 按前缀批量删除（用于写操作后让整类缓存失效）
     */
    void evictByPrefix(String prefix);

    /**
     * Redis 当前是否可用
     */
    boolean available();
}
