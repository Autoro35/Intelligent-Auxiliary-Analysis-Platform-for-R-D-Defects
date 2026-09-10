package com.defect.platform.config;

import com.defect.platform.common.ResultCode;
import com.defect.platform.common.annotation.RequireRole;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.common.context.LoginUser;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 角色权限切面：校验 {@link RequireRole} 标注的方法是否具备相应角色
 */
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requireRole)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        RoleEnum[] allowed = requireRole.value();
        // 未指定角色，仅需登录
        if (allowed.length == 0) {
            return joinPoint.proceed();
        }
        for (RoleEnum role : allowed) {
            if (role.getCode().equals(loginUser.getRole())) {
                return joinPoint.proceed();
            }
        }
        throw new BusinessException(ResultCode.FORBIDDEN);
    }
}
