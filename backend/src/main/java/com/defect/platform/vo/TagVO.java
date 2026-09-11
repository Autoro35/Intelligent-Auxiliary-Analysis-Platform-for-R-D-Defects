package com.defect.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 标签聚合项（标签名 + 出现次数），用于知识库标签云
 */
@Data
@AllArgsConstructor
public class TagVO {

    /** 标签名 */
    private String name;

    /** 出现次数 */
    private Long count;
}
