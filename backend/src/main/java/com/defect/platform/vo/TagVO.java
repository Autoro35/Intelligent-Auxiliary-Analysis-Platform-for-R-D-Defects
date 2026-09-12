package com.defect.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签聚合项（标签名 + 出现次数），用于知识库标签云
 * <p>需要无参构造：该对象会进缓存，反序列化时依赖默认构造函数</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagVO {

    /** 标签名 */
    private String name;

    /** 出现次数 */
    private Long count;
}
