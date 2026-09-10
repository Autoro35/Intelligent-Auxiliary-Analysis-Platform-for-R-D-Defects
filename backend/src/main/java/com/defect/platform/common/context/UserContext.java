package com.defect.platform.common.context;

/**
 * 当前登录用户上下文，基于 ThreadLocal 保存，随请求结束清理
 * <p>由 {@link com.defect.platform.config.JwtInterceptor} 写入，业务代码直接读取</p>
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前用户 ID，未登录返回 null */
    public static Long getUserId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    /** 获取当前用户名，未登录返回 null */
    public static String getUsername() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getUsername();
    }

    /** 获取当前用户角色，未登录返回 null */
    public static String getRole() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getRole();
    }

    /** 清理上下文，防止 ThreadLocal 内存泄漏 */
    public static void clear() {
        HOLDER.remove();
    }
}
