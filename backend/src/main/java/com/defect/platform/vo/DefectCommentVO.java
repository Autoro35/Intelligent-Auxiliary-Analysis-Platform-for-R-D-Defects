package com.defect.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 缺陷评论视图对象
 */
@Data
public class DefectCommentVO {

    private Long id;

    private Long defectId;

    private Long userId;

    private String username;

    private String nickname;

    private String content;

    private LocalDateTime createTime;
}
