package com.ticket.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> R<T> success() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> success(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> success(String msg, T data) {
        return build(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /**
     * 处理分页数据（绕过泛型推断问题）
     */
    @SuppressWarnings("unchecked")
    public static <T> R<T> page(Object data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), (T) data);
    }

    public static <T> R<T> fail() {
        return build(ResultCode.BUSINESS_ERROR.getCode(), ResultCode.BUSINESS_ERROR.getMsg(), null);
    }

    public static <T> R<T> fail(String msg) {
        return build(ResultCode.BUSINESS_ERROR.getCode(), ResultCode.BUSINESS_ERROR.getMsg(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMsg(), null);
    }

    public static <T> R<T> fail(Integer code, String msg) {
        return build(code, msg, null);
    }

    private static <T> R<T> build(Integer code, String msg, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
