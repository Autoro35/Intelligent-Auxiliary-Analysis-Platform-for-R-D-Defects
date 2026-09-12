package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.defect.platform.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 缓存服务实现（Redis）
 * <p>所有 Redis 操作都包在 try/catch 里：任何异常都降级为「直接回源」，不让缓存成为单点故障。</p>
 * <p>连续失败后会在 {@link #UNAVAILABLE_COOLDOWN_MS} 内跳过 Redis，避免每次请求都白等一个连接超时。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    /** 探测到 Redis 不可用后的冷却时间（毫秒），期间直接跳过缓存 */
    private static final long UNAVAILABLE_COOLDOWN_MS = 30_000L;

    /** 按前缀清理时单次扫描的上限，防止 key 过多时阻塞 */
    private static final long SCAN_LIMIT = 1000L;

    private final StringRedisTemplate redisTemplate;

    private volatile boolean available = true;
    private volatile long unavailableUntil = 0L;

    @Override
    public <T> T get(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        String cached = read(key);
        if (cached != null) {
            try {
                return JSONUtil.toBean(cached, type);
            } catch (Exception e) {
                // 缓存内容损坏或类型不兼容，丢弃后回源
                log.warn("缓存反序列化失败，将回源重建: key={}, err={}", key, e.getMessage());
                evict(key);
            }
        }
        T value = loader.get();
        write(key, ttl, value);
        return value;
    }

    @Override
    public <T> List<T> getList(String key, Duration ttl, Class<T> elementType, Supplier<List<T>> loader) {
        String cached = read(key);
        if (cached != null) {
            try {
                return JSONUtil.toList(cached, elementType);
            } catch (Exception e) {
                log.warn("缓存反序列化失败，将回源重建: key={}, err={}", key, e.getMessage());
                evict(key);
            }
        }
        List<T> value = loader.get();
        write(key, ttl, value);
        return value;
    }

    @Override
    public void evict(String key) {
        if (skipCache()) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            markUnavailable("删除缓存失败", e);
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        if (skipCache()) {
            return;
        }
        // 用 SCAN 而不是 KEYS，避免大 key 空间下阻塞 Redis
        long deleted = 0;
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(prefix + "*").count(200).build())) {
            while (cursor.hasNext() && deleted < SCAN_LIMIT) {
                redisTemplate.delete(cursor.next());
                deleted++;
            }
        } catch (Exception e) {
            markUnavailable("按前缀清理缓存失败", e);
        }
    }

    @Override
    public boolean available() {
        if (!skipCache()) {
            return true;
        }
        // 冷却期内主动再探一次，让 Redis 恢复后能自动重新启用
        try {
            redisTemplate.hasKey("__cache_probe__");
            markAvailable();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- 私有 ----

    private String read(String key) {
        if (skipCache() || StrUtil.isBlank(key)) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            markAvailable();
            return value;
        } catch (Exception e) {
            markUnavailable("读取缓存失败", e);
            return null;
        }
    }

    private void write(String key, Duration ttl, Object value) {
        if (skipCache() || StrUtil.isBlank(key) || value == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), ttl);
            markAvailable();
        } catch (Exception e) {
            markUnavailable("写入缓存失败", e);
        }
    }

    /** 处于不可用冷却期内则跳过缓存 */
    private boolean skipCache() {
        return System.currentTimeMillis() < unavailableUntil;
    }

    private void markUnavailable(String action, Exception e) {
        if (skipCache()) {
            return;
        }
        unavailableUntil = System.currentTimeMillis() + UNAVAILABLE_COOLDOWN_MS;
        available = false;
        log.warn("{}（Redis 不可用，将在 {}s 内直接跳过缓存）: {}", action, UNAVAILABLE_COOLDOWN_MS / 1000, e.getMessage());
    }

    private void markAvailable() {
        if (!available) {
            log.info("Redis 已恢复，缓存重新启用");
        }
        available = true;
        unavailableUntil = 0L;
    }
}
