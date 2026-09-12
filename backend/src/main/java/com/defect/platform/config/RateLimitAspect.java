package com.defect.platform.config;

import cn.hutool.core.util.StrUtil;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.annotation.RateLimit;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流切面
 * <p>固定窗口计数：为每个限流键维护「窗口起点 + 计数」，窗口过期则重新计数。
 * 采用近似实现——并发下计数可能略有偏差（同一毫秒内的竞争），
 * 对于防刷与成本控制场景足够，不追求精确的令牌桶语义。</p>
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /** 计数器数量上限，超过后清理过期窗口，避免内存无限增长 */
    private static final int MAX_COUNTERS = 10_000;

    /** 清理时判定「过期」的保守时长（毫秒），大于任何注解配置的窗口 */
    private static final long STALE_MILLIS = 3_600_000L;

    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint, rateLimit);
        if (!tryAcquire(key, rateLimit.limit(), rateLimit.window())) {
            log.warn("触发限流: key={}, limit={}/{}s", key, rateLimit.limit(), rateLimit.window());
            String message = StrUtil.blankToDefault(rateLimit.message(), ResultCode.TOO_MANY_REQUESTS.getMessage());
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), message);
        }
        return joinPoint.proceed();
    }

    // ---- 私有 ----

    /**
     * 构造限流键：维度值 + 方法全名，保证不同接口互不影响
     */
    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String method = joinPoint.getSignature().toShortString();
        String scope;
        switch (rateLimit.dimension()) {
            case GLOBAL:
                scope = "global";
                break;
            case IP:
                scope = "ip:" + clientIp();
                break;
            case USER:
            default:
                Long userId = UserContext.getUserId();
                // 登录等未认证接口拿不到用户，退化为按 IP 限流
                scope = userId != null ? "user:" + userId : "ip:" + clientIp();
                break;
        }
        return scope + "|" + method;
    }

    /**
     * 固定窗口计数，返回本次请求是否被放行
     */
    private boolean tryAcquire(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;
        Window window = counters.compute(key, (k, current) -> {
            if (current == null || now - current.start >= windowMillis) {
                return new Window(now, 1);
            }
            current.count++;
            return current;
        });
        if (counters.size() > MAX_COUNTERS) {
            pruneStale(now);
        }
        return window.count <= limit;
    }

    /** 清理长时间未再被访问的窗口 */
    private void pruneStale(long now) {
        int before = counters.size();
        counters.entrySet().removeIf(entry -> now - entry.getValue().start >= STALE_MILLIS);
        log.debug("限流计数器清理: {} -> {}", before, counters.size());
    }

    /**
     * 取客户端 IP；部署在 Nginx 之后时优先取转发链的第一个地址
     */
    private String clientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwarded)) {
            // X-Forwarded-For 可能是 "客户端IP, 代理1, 代理2"，取最左侧的真实客户端
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp.trim();
        }
        return StrUtil.blankToDefault(request.getRemoteAddr(), "unknown");
    }

    /**
     * 单个限流窗口
     */
    private static final class Window {

        /** 窗口起点（毫秒） */
        private final long start;

        /** 窗口内已计数 */
        private int count;

        private Window(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }
}
