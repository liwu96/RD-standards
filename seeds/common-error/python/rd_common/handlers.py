"""FastAPI 统一异常处理器注册。

用法（应用入口）：
    from rd_common.handlers import register_exception_handlers
    register_exception_handlers(app)
"""

from __future__ import annotations

import logging
from collections.abc import Iterable
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from .errors import BaseError, DEFAULT_MESSAGES, ErrorCode, HTTP_STATUS
from .responses import error

logger = logging.getLogger(__name__)
_REQUEST_ID_STATE = "request_id"
_REQUEST_ID_HEADER = "X-Request-Id"


def _request_id(request: Request) -> str:
    """Use the caller's ID or generate one once for this request."""
    header_rid = request.headers.get(_REQUEST_ID_HEADER)
    if header_rid and header_rid.strip():
        request_id = header_rid.strip()
    else:
        state_rid = getattr(request.state, _REQUEST_ID_STATE, None)
        request_id = state_rid.strip() if isinstance(state_rid, str) and state_rid.strip() else str(uuid4())
    setattr(request.state, _REQUEST_ID_STATE, request_id)
    return request_id


def _validation_details(exc: RequestValidationError) -> list[dict[str, str]]:
    details: list[dict[str, str]] = []
    for item in exc.errors():
        locations: Iterable[object] = item.get("loc") or ()
        parts = [str(part) for part in locations]
        # FastAPI prefixes locations with the input source. The public error
        # contract describes the actual field, so omit that transport detail.
        if len(parts) > 1 and parts[0] in {"body", "query", "path", "header", "cookie"}:
            parts = parts[1:]
        details.append(
            {
                "field": ".".join(parts) or "request",
                "reason": str(item.get("msg", "参数格式错误")),
            }
        )
    return details


def _validation_code(exc: RequestValidationError) -> ErrorCode:
    """Malformed JSON is a format error (A0102); other validation is A0100."""
    for item in exc.errors():
        error_type = str(item.get("type", "")).lower()
        if "json" in error_type and ("decode" in error_type or "invalid" in error_type):
            return ErrorCode.INVALID_FORMAT
    return ErrorCode.INVALID_PARAM


def _http_error_code(status_code: int) -> ErrorCode:
    return {
        400: ErrorCode.INVALID_PARAM,
        401: ErrorCode.UNAUTHORIZED,
        403: ErrorCode.ACCESS_DENIED,
        404: ErrorCode.NOT_FOUND,
        408: ErrorCode.REQUEST_TIMEOUT,
        409: ErrorCode.CONFLICT,
        410: ErrorCode.RESOURCE_GONE,
        422: ErrorCode.INVALID_PARAM,
        429: ErrorCode.RATE_LIMITED,
        502: ErrorCode.THIRD_PARTY_ERROR,
        503: ErrorCode.SERVICE_UNAVAILABLE,
    }.get(status_code, ErrorCode.INTERNAL_ERROR)


def _http_error_message(exc: StarletteHTTPException, error_code: ErrorCode) -> str | None:
    # HTTPException.detail may be an arbitrary object. Only a caller-provided
    # string is safe to expose as the envelope message.
    if isinstance(exc.detail, str) and exc.detail.strip():
        return exc.detail
    return DEFAULT_MESSAGES[error_code]


def register_exception_handlers(app: FastAPI) -> None:
    @app.middleware("http")
    async def request_id_middleware(request: Request, call_next):
        request_id = _request_id(request)
        response = await call_next(request)
        response.headers[_REQUEST_ID_HEADER] = request_id
        return response

    @app.exception_handler(BaseError)
    async def business_error_handler(request: Request, exc: BaseError) -> JSONResponse:
        request_id = _request_id(request)
        code = exc.error_code
        if code.value.startswith(("B", "C")):
            logger.error(
                "business error requestId=%s code=%s message=%s",
                request_id,
                code.value,
                exc.message,
                exc_info=(type(exc), exc, exc.__traceback__),
            )
        else:
            logger.warning(
                "business error requestId=%s code=%s message=%s",
                request_id,
                code.value,
                exc.message,
            )
        response = JSONResponse(
            status_code=HTTP_STATUS[code],
            content=error(code, exc.message, exc.details, request_id),
        )
        response.headers[_REQUEST_ID_HEADER] = request_id
        return response

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
        request_id = _request_id(request)
        code = _validation_code(exc)
        details = _validation_details(exc)
        logger.warning(
            "request validation failed requestId=%s code=%s details=%s",
            request_id,
            code.value,
            details,
        )
        response = JSONResponse(
            status_code=HTTP_STATUS[code],
            content=error(code, None, details, request_id),
        )
        response.headers[_REQUEST_ID_HEADER] = request_id
        return response

    @app.exception_handler(StarletteHTTPException)
    async def http_error_handler(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        request_id = _request_id(request)
        code = _http_error_code(exc.status_code)
        if exc.status_code >= 500:
            logger.error(
                "http error requestId=%s status=%s code=%s",
                request_id,
                exc.status_code,
                code.value,
                exc_info=(type(exc), exc, exc.__traceback__),
            )
        else:
            logger.warning(
                "http error requestId=%s status=%s code=%s",
                request_id,
                exc.status_code,
                code.value,
            )
        response = JSONResponse(
            status_code=HTTP_STATUS.get(code, 500),
            content=error(code, _http_error_message(exc, code), None, request_id),
        )
        response.headers[_REQUEST_ID_HEADER] = request_id
        return response

    @app.exception_handler(Exception)
    async def unknown_error_handler(request: Request, exc: Exception) -> JSONResponse:
        # 未分类异常兜底：B0001。堆栈只进日志，禁止透出（见 status-codes.md §2）。
        request_id = _request_id(request)
        logger.exception("unexpected error requestId=%s", request_id)
        response = JSONResponse(
            status_code=500,
            content=error(ErrorCode.INTERNAL_ERROR, None, None, request_id),
        )
        response.headers[_REQUEST_ID_HEADER] = request_id
        return response
