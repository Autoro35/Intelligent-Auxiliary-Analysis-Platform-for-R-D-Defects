package com.defect.platform.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果体，所有接口返回该结构，保证前后端字段严格对齐
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200 成功，其余为失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 时间戳（毫秒） */
    private Long timestamp;

    private Result() {
    }

    private static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public static <T> Result<T> success() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return build(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail(String message) {
        return build(ResultCode.ERROR.getCode(), message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return build(code, message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }
}
