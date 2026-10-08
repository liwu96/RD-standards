package com.rd.common.error;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 统一响应包 envelope：{code, message, data, requestId, serverTime}。
 * 结构见 rules/api/api-request-response.md §2.1；协议为 lowerCamelCase，
 * 与 Java 属性同名，无需额外 Jackson 配置。
 * 失败时 data 通常为 null；A0100 参数校验失败可在 data.details 放字段级详情（见 GlobalExceptionHandler）。
 */
public class Result<T> {

    private String code;
    private String message;
    private T data;
    private String requestId;
    private String serverTime;

    public static <T> Result<T> ok(T data, String requestId) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.OK.getCode();
        r.message = "success";
        r.data = data;
        return fillBase(r, requestId);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String userMessage, T data, String requestId) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = userMessage != null ? userMessage : errorCode.getDefaultMessage();
        r.data = data; // 通常为 null；A0100 场景传 {"details": [...]}
        return fillBase(r, requestId);
    }

    private static <T> Result<T> fillBase(Result<T> r, String requestId) {
        r.requestId = requestId != null && !requestId.trim().isEmpty()
                ? requestId
                : UUID.randomUUID().toString();
        r.serverTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return r;
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

    public String getRequestId() {
        return requestId;
    }

    public String getServerTime() {
        return serverTime;
    }
}
