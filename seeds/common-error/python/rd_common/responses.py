"""统一响应包 envelope 构造器。

结构见 rules/api/api-request-response.md §2.1：
{code, message, data, requestId, serverTime}；失败时 data 为 null
（A0100 的字段级详情使用 data.details 结构）。
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from .errors import DEFAULT_MESSAGES, ErrorCode


def _now_rfc3339() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def success(data: Any = None, request_id: str | None = None) -> dict[str, Any]:
    """成功响应：code="0"。"""
    return {
        "code": ErrorCode.OK.value,
        "message": "success",
        "data": data,
        "requestId": request_id,
        "serverTime": _now_rfc3339(),
    }


def page(
    items: list[Any],
    total: int,
    page: int,
    page_size: int,
    request_id: str | None = None,
) -> dict[str, Any]:
    """列表响应：data = {total, page, pageSize, list}（见出入参规范 §2.2）。"""
    return success(
        data={"total": total, "page": page, "pageSize": page_size, "list": items},
        request_id=request_id,
    )


def error(
    error_code: ErrorCode,
    message: str | None = None,
    details: list[dict[str, str]] | None = None,
    request_id: str | None = None,
) -> dict[str, Any]:
    """失败响应：data 通常为 None；传 details 时包装为 {"details": [...]}（A0100）。"""
    data = {"details": details} if details else None
    return {
        "code": error_code.value,
        "message": message or DEFAULT_MESSAGES[error_code],
        "data": data,
        "requestId": request_id,
        "serverTime": _now_rfc3339(),
    }
