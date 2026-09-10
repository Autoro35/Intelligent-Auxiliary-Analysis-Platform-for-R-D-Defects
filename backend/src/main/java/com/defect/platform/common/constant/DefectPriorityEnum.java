package com.defect.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷优先级枚举
 */
@Getter
@AllArgsConstructor
public enum DefectPriorityEnum {

    URGENT("URGENT", "紧急"),
    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String desc;

    /**
     * 根据 code 获取中文描述，未匹配时返回原值
     */
    public static String descOf(String code) {
        for (DefectPriorityEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return code;
    }
}
