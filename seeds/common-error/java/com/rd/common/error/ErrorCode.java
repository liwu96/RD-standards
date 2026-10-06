package com.rd.common.error;

/**
 * 全局错误码枚举，与 rules/api/status-codes.md 登记表保持同步。
 * 编码方案：1 位大类字母（A 用户端 / B 系统端 / C 三方）+ 4 位数字。
 * 新增错误码必须先在规范登记，再在此扩展；已发布语义不可变更。
 */
public enum ErrorCode {
    OK("0", "success"),

    // ---------- A 用户端错误 ----------
    INVALID_PARAM("A0100", "参数校验失败"),
    MISSING_PARAM("A0101", "缺少必填参数"),
    INVALID_FORMAT("A0102", "参数格式错误"),
    UNAUTHORIZED("A0200", "请先登录"),
    TOKEN_EXPIRED("A0201", "登录已过期，请重新登录"),
    TOKEN_INVALID("A0202", "登录态校验失败"),
    ACCESS_DENIED("A0300", "无权访问该接口"),
    FORBIDDEN_RESOURCE("A0301", "无权操作该资源"),
    NOT_FOUND("A0400", "资源不存在"),
    CONFLICT("A0500", "记录已存在"),
    VERSION_CONFLICT("A0501", "数据已被修改，请刷新后重试"),
    RESOURCE_GONE("A0600", "资源已失效"),
    RATE_LIMITED("A0700", "请求过于频繁，请稍后再试"),
    RISK_REJECTED("A0701", "操作过于频繁，请稍后再试"),
    REQUEST_TIMEOUT("A0800", "请求超时"),

    // ---------- B 系统端错误 ----------
    INTERNAL_ERROR("B0001", "服务开小差了，请稍后再试"),
    DB_ERROR("B0100", "数据服务异常"),
    DB_DEADLOCK("B0101", "数据冲突，请重试"),
    CACHE_ERROR("B0200", "缓存服务异常"),
    SERVICE_UNAVAILABLE("B0201", "服务暂不可用"),
    MQ_ERROR("B0300", "消息投递失败"),
    CONFIG_ERROR("B0400", "服务配置异常"),

    // ---------- C 第三方服务错误 ----------
    THIRD_PARTY_ERROR("C0000", "第三方服务异常"),
    PAYMENT_ERROR("C0100", "支付渠道调用失败"),
    SMS_EMAIL_ERROR("C0200", "短信/邮件发送失败"),
    OSS_ERROR("C0300", "对象存储调用失败"),
    EXTERNAL_HTTP_ERROR("C0400", "外部服务调用失败");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
