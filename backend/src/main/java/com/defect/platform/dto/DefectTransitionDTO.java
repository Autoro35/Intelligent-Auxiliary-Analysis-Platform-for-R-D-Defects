package com.defect.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 缺陷状态流转请求参数
 */
@Data
public class DefectTransitionDTO {

    /** 操作类型：ASSIGN/START/RESOLVE/CLOSE/REJECT/REOPEN */
    @NotBlank(message = "操作类型不能为空")
    private String action;

    /** 分配处理人用户 ID（ASSIGN 时必填） */
    private Long assigneeId;

    /** 解决方案（RESOLVE 时必填） */
    private String solution;

    /** 操作备注（可选，用于日志留痕） */
    private String remark;
}
