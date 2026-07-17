package com.pyin.plugin.common.api;

public class Result<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private Result(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, "OK", "success", data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(false, code, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
