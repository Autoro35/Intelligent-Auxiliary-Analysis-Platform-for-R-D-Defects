package com.defect.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目内角色枚举（区别于系统全局角色 RoleEnum）
 */
@Getter
@AllArgsConstructor
public enum ProjectRoleEnum {

    OWNER("OWNER", "负责人"),
    DEV("DEV", "开发"),
    TESTER("TESTER", "测试"),
    VIEWER("VIEWER", "访客");

    private final String code;
    private final String desc;

    /**
     * 根据 code 获取中文描述，未匹配时返回原值
     */
    public static String descOf(String code) {
        for (ProjectRoleEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return code;
    }
}
