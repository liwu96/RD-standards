package com.rd.common.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Spring 全局异常处理：业务异常 -> 对应 HTTP 码 + envelope；
 * 未分类异常 -> B0001（记录 error 日志含堆栈，对外只返回通用提示）。
 * HTTP 映射表见 rules/api/status-codes.md §2。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** HTTP 状态映射：按规范登记表维护，新增错误码时同步。 */
    private static HttpStatus httpStatusOf(ErrorCode ec) {
        switch (ec) {
            case INVALID_PARAM:
            case MISSING_PARAM:
            case INVALID_FORMAT:
                return HttpStatus.BAD_REQUEST;          // 400
            case UNAUTHORIZED:
            case TOKEN_EXPIRED:
            case TOKEN_INVALID:
                return HttpStatus.UNAUTHORIZED;          // 401
            case ACCESS_DENIED:
            case FORBIDDEN_RESOURCE:
                return HttpStatus.FORBIDDEN;             // 403
            case NOT_FOUND:
                return HttpStatus.NOT_FOUND;             // 404
            case CONFLICT:
            case VERSION_CONFLICT:
                return HttpStatus.CONFLICT;              // 409
            case RESOURCE_GONE:
                return HttpStatus.GONE;                  // 410
            case REQUEST_TIMEOUT:
                return HttpStatus.REQUEST_TIMEOUT;       // 408
            case RATE_LIMITED:
            case RISK_REJECTED:
                return HttpStatus.TOO_MANY_REQUESTS;     // 429
            case SERVICE_UNAVAILABLE:
                return HttpStatus.SERVICE_UNAVAILABLE;   // 503
            case THIRD_PARTY_ERROR:
            case PAYMENT_ERROR:
            case SMS_EMAIL_ERROR:
            case OSS_ERROR:
            case EXTERNAL_HTTP_ERROR:
                return HttpStatus.BAD_GATEWAY;           // 502
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR; // 500
        }
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusiness(BusinessException e) {
        if (e.getErrorCode() == ErrorCode.DB_ERROR
                || e.getErrorCode().getCode().startsWith("B")) {
            logger.error("business error: {}", e.getMessage(), e); // B 类记 error
        }
        Object data = null;
        if (e.getDetails() != null) {
            Map<String, Object> wrap = new HashMap<>(2);
            wrap.put("details", e.getDetails());
            data = wrap;
        }
        return ResponseEntity.status(httpStatusOf(e.getErrorCode()))
                .body(Result.error(e.getErrorCode(), e.getUserMessage(), data, currentRequestId()));
    }

    /** 参数校验失败（@Valid）：转 A0100 + 字段级 details。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "reason", String.valueOf(fe.getDefaultMessage())))
                .toList();
        Map<String, Object> data = Map.of("details", details);
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.INVALID_PARAM, null, data, currentRequestId()));
    }

    /** 未分类异常兜底：B0001。堆栈只进日志，禁止透出。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleUnknown(Exception e) {
        logger.error("unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.INTERNAL_ERROR, null, null, currentRequestId()));
    }

    private String currentRequestId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String id = req.getHeader("X-Request-Id"); // 与出入参规范 §1.1 对应，未传则生成
        return id != null ? id : java.util.UUID.randomUUID().toString();
    }
}
