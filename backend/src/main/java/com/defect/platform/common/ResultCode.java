package com.defect.platform.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 * <p>1xxx 为业务自定义错误码，HTTP 状态码沿用约定值</p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ---- 通用 ----
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "系统内部错误"),

    // ---- 用户/认证 1xxx ----
    USERNAME_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),

    // ---- 项目 2xxx ----
    PROJECT_NOT_FOUND(2001, "项目不存在"),
    PROJECT_CODE_EXISTS(2002, "项目编码已存在"),
    PROJECT_MEMBER_EXISTS(2003, "该用户已是项目成员"),
    PROJECT_NOT_MEMBER(2004, "非项目成员，无权操作"),

    // ---- 缺陷 3xxx ----
    DEFECT_NOT_FOUND(3001, "缺陷不存在"),
    DEFECT_STATUS_ILLEGAL(3002, "非法的状态流转"),

    // ---- 知识库 4xxx ----
    KNOWLEDGE_NOT_FOUND(4001, "知识库条目不存在"),
    KNOWLEDGE_EXISTS(4002, "该缺陷已沉淀过知识"),

    // ---- AI 能力 5xxx ----
    AI_NOT_CONFIGURED(5001, "AI 服务未配置（缺少 DeepSeek API Key）"),
    AI_CALL_FAILED(5002, "AI 服务调用失败，请稍后重试"),
    AI_RESPONSE_INVALID(5003, "AI 返回内容格式异常，无法解析"),
    VECTOR_STORE_UNAVAILABLE(5004, "向量库不可用");

    private final Integer code;
    private final String message;
}
