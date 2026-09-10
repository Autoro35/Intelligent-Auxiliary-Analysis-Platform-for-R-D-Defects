package com.defect.platform.common.annotation;

import com.defect.platform.common.constant.RoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解，标注在 Controller 方法上，配合 {@link com.defect.platform.config.PermissionAspect} 校验
 * <p>value 为空数组表示仅需登录即可访问；否则需具备其中任一角色</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色集合，缺省为空（仅需登录） */
    RoleEnum[] value() default {};
}
