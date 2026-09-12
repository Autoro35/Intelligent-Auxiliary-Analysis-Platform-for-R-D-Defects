package com.defect.platform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * <p>基于内存的固定窗口计数，无需引入 Redis，单机部署开箱即用。
 * 多实例部署时每个实例各自计数，如需全局限流需换用 Redis 实现。</p>
 * <p>典型用法：登录接口按 IP 限流防止撞库，AI 接口按用户限流控制大模型调用成本。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口内允许的最大请求次数 */
    int limit() default 20;

    /** 时间窗口长度（秒） */
    int window() default 60;

    /** 限流维度 */
    Dimension dimension() default Dimension.USER;

    /** 触发限流时的提示语，留空使用默认文案 */
    String message() default "";

    /**
     * 限流维度
     */
    enum Dimension {
        /** 按登录用户；未登录（如登录接口）时自动退化为按 IP */
        USER,
        /** 按客户端 IP */
        IP,
        /** 全局共享一个计数器 */
        GLOBAL
    }
}
