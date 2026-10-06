package com.rd.common.error;

/**
 * 业务异常：携带 ErrorCode，由 GlobalExceptionHandler 统一转为 Response envelope。
 * 用法：throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
 * message 面向最终用户；排查信息通过日志 + requestId 关联，禁止放入异常消息。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String userMessage;
    /** 字段级错误详情，如 A0100 的 [{field, reason}] 列表；可为 null。 */
    private final transient Object details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String userMessage) {
        this(errorCode, userMessage, null);
    }

    public BusinessException(ErrorCode errorCode, String userMessage, Object details) {
        super(errorCode.getCode() + ": " + userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public Object getDetails() {
        return details;
    }
}
