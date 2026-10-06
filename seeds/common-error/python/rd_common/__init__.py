"""公共错误码包（种子代码）：错误码枚举、业务异常、响应 envelope 与 FastAPI 处理器。

接入说明见 seeds/common-error/README.md；错误码登记表见 rules/api/status-codes.md。
"""

from .errors import BusinessError, DEFAULT_MESSAGES, ErrorCode, HTTP_STATUS
from .responses import error, page, success

__all__ = [
    "BusinessError",
    "ErrorCode",
    "DEFAULT_MESSAGES",
    "HTTP_STATUS",
    "success",
    "page",
    "error",
]
