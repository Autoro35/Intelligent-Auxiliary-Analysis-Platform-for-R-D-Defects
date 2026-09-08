package com.defect.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统内置角色枚举
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    /** 管理员 */
    ADMIN("ADMIN", "管理员"),
    /** 测试人员 */
    TESTER("TESTER", "测试"),
    /** 开发人员 */
    DEVELOPER("DEVELOPER", "开发"),
    /** 访客 */
    GUEST("GUEST", "访客");

    private final String code;
    private final String desc;
}
