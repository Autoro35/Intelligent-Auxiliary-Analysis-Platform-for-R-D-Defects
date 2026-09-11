package com.defect.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统计分布项（维度编码 + 中文描述 + 数量）
 */
@Data
@AllArgsConstructor
public class StatItemVO {

    /** 维度编码（如状态/类型/优先级/严重程度编码） */
    private String code;

    /** 中文描述 */
    private String desc;

    /** 数量 */
    private Long count;
}
