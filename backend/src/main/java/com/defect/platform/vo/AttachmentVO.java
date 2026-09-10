package com.defect.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件视图对象（不暴露服务器存储路径）
 */
@Data
public class AttachmentVO {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String contentType;

    private Long uploaderId;

    private String uploaderName;

    private LocalDateTime createTime;
}
