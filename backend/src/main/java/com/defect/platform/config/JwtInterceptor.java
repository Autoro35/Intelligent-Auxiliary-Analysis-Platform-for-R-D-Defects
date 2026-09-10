package com.defect.platform.config;

import com.defect.platform.common.ResultCode;
import com.defect.platform.common.context.LoginUser;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器：校验请求头中的 token，解析后写入用户上下文
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(jwtUtil.getHeader());
        String prefix = jwtUtil.getPrefix();
        if (header == null || !header.startsWith(prefix)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = header.substring(prefix.length());
        if (!jwtUtil.validate(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        LoginUser loginUser = new LoginUser(
                jwtUtil.getUserId(token),
                jwtUtil.getUsername(token),
                jwtUtil.getRole(token));
        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
