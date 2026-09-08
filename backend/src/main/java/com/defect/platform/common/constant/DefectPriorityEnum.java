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
}
