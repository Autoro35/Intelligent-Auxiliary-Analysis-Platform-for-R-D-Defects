package com.defect.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷状态枚举
 * <p>流转链路：NEW(新建) → ASSIGNED(已分配) → PROCESSING(处理中) → PENDING_RETEST(待复测) → CLOSED(已关闭) / REJECTED(已驳回)</p>
 */
@Getter
@AllArgsConstructor
public enum DefectStatusEnum {

    NEW("NEW", "新建"),
    ASSIGNED("ASSIGNED", "已分配"),
    PROCESSING("PROCESSING", "处理中"),
    PENDING_RETEST("PENDING_RETEST", "待复测"),
    CLOSED("CLOSED", "已关闭"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String desc;
}
