package com.defect.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 附件实体，对应表 t_attachment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_attachment")
public class Attachment extends BaseEntity {

    /** 业务类型：DEFECT/COMMENT */
    private String businessType;

    /** 业务主键 ID */
    private Long businessId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型 */
    private String contentType;

    /** 上传人用户 ID */
    private Long uploaderId;
}
