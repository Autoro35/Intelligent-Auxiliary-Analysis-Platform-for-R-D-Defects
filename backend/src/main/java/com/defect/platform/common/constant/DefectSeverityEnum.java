package com.defect.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷严重程度枚举
 */
@Getter
@AllArgsConstructor
public enum DefectSeverityEnum {

    BLOCKER("BLOCKER", "致命"),
    CRITICAL("CRITICAL", "严重"),
    MAJOR("MAJOR", "一般"),
    MINOR("MINOR", "轻微");

    private final String code;
    private final String desc;
}
