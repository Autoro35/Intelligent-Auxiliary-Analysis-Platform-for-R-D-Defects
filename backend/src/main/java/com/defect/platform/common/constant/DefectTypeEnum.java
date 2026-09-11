package com.defect.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷类型枚举
 */
@Getter
@AllArgsConstructor
public enum DefectTypeEnum {

    FUNCTIONAL("FUNCTIONAL", "功能Bug"),
    PERFORMANCE("PERFORMANCE", "性能问题"),
    UI("UI", "UI异常"),
    COMPATIBILITY("COMPATIBILITY", "兼容性问题"),
    OPTIMIZATION("OPTIMIZATION", "建议优化");

    private final String code;
    private final String desc;

    /**
     * 根据 code 获取中文描述，未匹配时返回原值
     */
    public static String descOf(String code) {
        for (DefectTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return code;
    }
}
