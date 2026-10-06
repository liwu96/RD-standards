"""全局错误码与业务异常。

与 rules/api/status-codes.md 登记表保持同步：
新增错误码 = 先改规范登记表（PR）→ 再扩展此处枚举；已发布语义不可变更。
"""

from __future__ import annotations

from enum import Enum


class ErrorCode(str, Enum):
    """编码方案：1 位大类字母（A 用户端 / B 系统端 / C 三方）+ 4 位数字。"""

    OK = "0"

    # ---------- A 用户端错误 ----------
    INVALID_PARAM = "A0100"
    MISSING_PARAM = "A0101"
    INVALID_FORMAT = "A0102"
    UNAUTHORIZED = "A0200"
    TOKEN_EXPIRED = "A0201"
    TOKEN_INVALID = "A0202"
    ACCESS_DENIED = "A0300"
    FORBIDDEN_RESOURCE = "A0301"
    NOT_FOUND = "A0400"
    CONFLICT = "A0500"
    VERSION_CONFLICT = "A0501"
    RESOURCE_GONE = "A0600"
    RATE_LIMITED = "A0700"
    RISK_REJECTED = "A0701"
    REQUEST_TIMEOUT = "A0800"

    # ---------- B 系统端错误 ----------
    INTERNAL_ERROR = "B0001"
    DB_ERROR = "B0100"
    DB_DEADLOCK = "B0101"
    CACHE_ERROR = "B0200"
    SERVICE_UNAVAILABLE = "B0201"
    MQ_ERROR = "B0300"
    CONFIG_ERROR = "B0400"

    # ---------- C 第三方服务错误 ----------
    THIRD_PARTY_ERROR = "C0000"
    PAYMENT_ERROR = "C0100"
    SMS_EMAIL_ERROR = "C0200"
    OSS_ERROR = "C0300"
    EXTERNAL_HTTP_ERROR = "C0400"


DEFAULT_MESSAGES: dict[ErrorCode, str] = {
    ErrorCode.OK: "success",
    ErrorCode.INVALID_PARAM: "参数校验失败",
    ErrorCode.MISSING_PARAM: "缺少必填参数",
    ErrorCode.INVALID_FORMAT: "参数格式错误",
    ErrorCode.UNAUTHORIZED: "请先登录",
    ErrorCode.TOKEN_EXPIRED: "登录已过期，请重新登录",
    ErrorCode.TOKEN_INVALID: "登录态校验失败",
    ErrorCode.ACCESS_DENIED: "无权访问该接口",
    ErrorCode.FORBIDDEN_RESOURCE: "无权操作该资源",
    ErrorCode.NOT_FOUND: "资源不存在",
    ErrorCode.CONFLICT: "记录已存在",
    ErrorCode.VERSION_CONFLICT: "数据已被修改，请刷新后重试",
    ErrorCode.RESOURCE_GONE: "资源已失效",
    ErrorCode.RATE_LIMITED: "请求过于频繁，请稍后再试",
    ErrorCode.RISK_REJECTED: "操作过于频繁，请稍后再试",
    ErrorCode.REQUEST_TIMEOUT: "请求超时",
    ErrorCode.INTERNAL_ERROR: "服务开小差了，请稍后再试",
    ErrorCode.DB_ERROR: "数据服务异常",
    ErrorCode.DB_DEADLOCK: "数据冲突，请重试",
    ErrorCode.CACHE_ERROR: "缓存服务异常",
    ErrorCode.SERVICE_UNAVAILABLE: "服务暂不可用",
    ErrorCode.MQ_ERROR: "消息投递失败",
    ErrorCode.CONFIG_ERROR: "服务配置异常",
    ErrorCode.THIRD_PARTY_ERROR: "第三方服务异常",
    ErrorCode.PAYMENT_ERROR: "支付渠道调用失败",
    ErrorCode.SMS_EMAIL_ERROR: "短信/邮件发送失败",
    ErrorCode.OSS_ERROR: "对象存储调用失败",
    ErrorCode.EXTERNAL_HTTP_ERROR: "外部服务调用失败",
}

# HTTP 状态映射，与 rules/api/status-codes.md §2 登记表一致
HTTP_STATUS: dict[ErrorCode, int] = {
    ErrorCode.INVALID_PARAM: 400,
    ErrorCode.MISSING_PARAM: 400,
    ErrorCode.INVALID_FORMAT: 400,
    ErrorCode.UNAUTHORIZED: 401,
    ErrorCode.TOKEN_EXPIRED: 401,
    ErrorCode.TOKEN_INVALID: 401,
    ErrorCode.ACCESS_DENIED: 403,
    ErrorCode.FORBIDDEN_RESOURCE: 403,
    ErrorCode.NOT_FOUND: 404,
    ErrorCode.CONFLICT: 409,
    ErrorCode.VERSION_CONFLICT: 409,
    ErrorCode.RESOURCE_GONE: 410,
    ErrorCode.REQUEST_TIMEOUT: 408,
    ErrorCode.RATE_LIMITED: 429,
    ErrorCode.RISK_REJECTED: 429,
    ErrorCode.INTERNAL_ERROR: 500,
    ErrorCode.DB_ERROR: 500,
    ErrorCode.DB_DEADLOCK: 500,
    ErrorCode.CACHE_ERROR: 500,
    ErrorCode.MQ_ERROR: 500,
    ErrorCode.CONFIG_ERROR: 500,
    ErrorCode.SERVICE_UNAVAILABLE: 503,
    ErrorCode.THIRD_PARTY_ERROR: 502,
    ErrorCode.PAYMENT_ERROR: 502,
    ErrorCode.SMS_EMAIL_ERROR: 502,
    ErrorCode.OSS_ERROR: 502,
    ErrorCode.EXTERNAL_HTTP_ERROR: 502,
}


class BusinessError(Exception):
    """业务异常：携带 ErrorCode，由统一异常处理器转为 Response envelope。

    用法：raise BusinessError(ErrorCode.NOT_FOUND, message="订单不存在")
    message 面向最终用户；排查信息通过日志 + requestId 关联，禁止放入异常消息。
    details 用于字段级错误（如 A0100 的 [{"field": ..., "reason": ...}]）。
    """

    def __init__(
        self,
        error_code: ErrorCode,
        message: str | None = None,
        details: list[dict[str, str]] | None = None,
    ) -> None:
        self.error_code = error_code
        self.message = message or DEFAULT_MESSAGES[error_code]
        self.details = details
        super().__init__(f"{error_code.value}: {self.message}")
