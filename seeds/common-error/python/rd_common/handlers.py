"""FastAPI 统一异常处理器注册。

用法（应用入口）：
    from rd_common.handlers import register_exception_handlers
    register_exception_handlers(app)
"""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .errors import BusinessError, ErrorCode, HTTP_STATUS
from .responses import error

logger = logging.getLogger(__name__)


def _request_id(request: Request) -> str:
    """与出入参规范 §1.1 对应：优先透传 X-Request-Id，未传则取中间件生成值。"""
    rid = request.headers.get("X-Request-Id")
    if rid:
        return rid
    state_rid = getattr(request.state, "request_id", None)
    return state_rid or ""


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(BusinessError)
    async def business_error_handler(request: Request, exc: BusinessError) -> JSONResponse:
        if exc.error_code.value.startswith("B"):
            logger.error("business error: %s", exc, exc_info=True)  # B 类记 error
        return JSONResponse(
            status_code=HTTP_STATUS[exc.error_code],
            content=error(exc.error_code, exc.message, exc.details, _request_id(request)),
        )

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
        details = [
            {"field": ".".join(str(loc) for loc in err.get("loc", [])), "reason": str(err.get("msg", ""))}
            for err in exc.errors()
        ]
        return JSONResponse(
            status_code=400,
            content=error(ErrorCode.INVALID_PARAM, None, details, _request_id(request)),
        )

    @app.exception_handler(Exception)
    async def unknown_error_handler(request: Request, exc: Exception) -> JSONResponse:
        # 未分类异常兜底：B0001。堆栈只进日志，禁止透出（见 status-codes.md §2）。
        logger.exception("unexpected error")
        return JSONResponse(
            status_code=500,
            content=error(ErrorCode.INTERNAL_ERROR, None, None, _request_id(request)),
        )
