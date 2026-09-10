package com.defect.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 缺陷评论请求参数
 */
@Data
public class DefectCommentDTO {

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
