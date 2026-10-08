package com.rd.common.error;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

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
        String requestId = currentRequestId();
        if (e.getErrorCode().getCode().startsWith("B")
                || e.getErrorCode().getCode().startsWith("C")) {
            logger.error("business error requestId={} code={} message={}", requestId,
                    e.getErrorCode().getCode(), e.getUserMessage(), e);
        } else {
            logger.warn("business error requestId={} code={} message={}", requestId,
                    e.getErrorCode().getCode(), e.getUserMessage());
        }
        Object data = null;
        if (e.getDetails() != null) {
            Map<String, Object> wrap = new HashMap<>(2);
            wrap.put("details", e.getDetails());
            data = wrap;
        }
        return response(httpStatusOf(e.getErrorCode()),
                Result.<Object>error(e.getErrorCode(), e.getUserMessage(), data, requestId), requestId);
    }

    /** 参数校验失败（@Valid）：转 A0100 + 字段级 details。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidation(MethodArgumentNotValidException e) {
        String requestId = currentRequestId();
        List<Map<String, String>> details = fieldDetails(e.getBindingResult().getFieldErrors());
        logger.warn("request validation failed requestId={} code={} details={}", requestId,
                ErrorCode.INVALID_PARAM.getCode(), details);
        Map<String, Object> data = new HashMap<>(2);
        data.put("details", details);
        return response(HttpStatus.BAD_REQUEST,
                Result.<Object>error(ErrorCode.INVALID_PARAM, null, data, requestId), requestId);
    }

    /** 表单、查询参数等绑定校验失败：统一转 A0100。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBind(BindException e) {
        String requestId = currentRequestId();
        List<Map<String, String>> details = fieldDetails(e.getBindingResult().getFieldErrors());
        logger.warn("request binding failed requestId={} code={} details={}", requestId,
                ErrorCode.INVALID_PARAM.getCode(), details);
        Map<String, Object> data = new HashMap<>(2);
        data.put("details", details);
        return response(HttpStatus.BAD_REQUEST,
                Result.<Object>error(ErrorCode.INVALID_PARAM, null, data, requestId), requestId);
    }

    /** 方法参数约束失败：统一转 A0100。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraintViolation(ConstraintViolationException e) {
        String requestId = currentRequestId();
        List<Map<String, String>> details = new ArrayList<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            Map<String, String> item = new HashMap<>(2);
            item.put("field", String.valueOf(violation.getPropertyPath()));
            item.put("reason", String.valueOf(violation.getMessage()));
            details.add(item);
        }
        logger.warn("constraint validation failed requestId={} code={} details={}", requestId,
                ErrorCode.INVALID_PARAM.getCode(), details);
        Map<String, Object> data = new HashMap<>(2);
        data.put("details", details);
        return response(HttpStatus.BAD_REQUEST,
                Result.<Object>error(ErrorCode.INVALID_PARAM, null, data, requestId), requestId);
    }

    /** 请求体不是合法 JSON 或参数类型不是合法格式：转 A0102。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Object>> handleUnreadableMessage(HttpMessageNotReadableException e) {
        String requestId = currentRequestId();
        logger.warn("request body is not readable requestId={} code={}", requestId,
                ErrorCode.INVALID_FORMAT.getCode());
        return response(HttpStatus.BAD_REQUEST,
                Result.<Object>error(ErrorCode.INVALID_FORMAT, null, null, requestId), requestId);
    }

    /** 缺少必填请求参数：转 A0101。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Object>> handleMissingParameter(MissingServletRequestParameterException e) {
        String requestId = currentRequestId();
        String message = "缺少必填参数: " + e.getParameterName();
        logger.warn("missing request parameter requestId={} code={} parameter={}", requestId,
                ErrorCode.MISSING_PARAM.getCode(), e.getParameterName());
        return response(HttpStatus.BAD_REQUEST,
                Result.<Object>error(ErrorCode.MISSING_PARAM, message, null, requestId), requestId);
    }

    /** 请求参数类型不匹配：转 A0102。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String requestId = currentRequestId();
        String message = "参数格式错误: " + e.getName();
        logger.warn("request parameter format failed requestId={} code={} parameter={}", requestId,
                ErrorCode.INVALID_FORMAT.getCode(), e.getName());
        return response(HttpStatus.BAD_REQUEST,
                Result.<Object>error(ErrorCode.INVALID_FORMAT, message, null, requestId), requestId);
    }

    /** 业务代码主动抛出的 HTTP 状态异常也必须使用统一 envelope。 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Object>> handleStatus(ResponseStatusException e) {
        String requestId = currentRequestId();
        HttpStatus status = responseStatus(e);
        ErrorCode errorCode = errorCodeOf(status);
        String message = status.is4xxClientError() ? e.getReason() : null;
        if (status.is5xxServerError()) {
            logger.error("http status exception requestId={} status={} code={}", requestId,
                    status.value(), errorCode.getCode(), e);
        } else {
            logger.warn("http status exception requestId={} status={} code={}", requestId,
                    status.value(), errorCode.getCode());
        }
        return response(status,
                Result.<Object>error(errorCode, message, null, requestId), requestId);
    }

    /** 未分类异常兜底：B0001。堆栈只进日志，禁止透出。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleUnknown(Exception e) {
        String requestId = currentRequestId();
        logger.error("unexpected error requestId={}", requestId, e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                Result.<Object>error(ErrorCode.INTERNAL_ERROR, null, null, requestId), requestId);
    }

    private String currentRequestId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return UUID.randomUUID().toString();
        }
        HttpServletRequest req = attrs.getRequest();
        Object existing = req.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String && !((String) existing).trim().isEmpty()) {
            return (String) existing;
        }
        String id = req.getHeader("X-Request-Id"); // 与出入参规范 §1.1 对应，未传则生成
        if (id != null) {
            id = id.trim();
        }
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        req.setAttribute(REQUEST_ID_ATTRIBUTE, id);
        return id;
    }

    private static final String REQUEST_ID_ATTRIBUTE = GlobalExceptionHandler.class.getName() + ".requestId";

    private static ResponseEntity<Result<Object>> response(
            HttpStatus status, Result<Object> body, String requestId) {
        return ResponseEntity.status(status)
                .header("X-Request-Id", requestId)
                .body(body);
    }

    private static List<Map<String, String>> fieldDetails(List<FieldError> errors) {
        List<Map<String, String>> details = new ArrayList<>();
        for (FieldError error : errors) {
            Map<String, String> item = new HashMap<>(2);
            item.put("field", error.getField());
            item.put("reason", String.valueOf(error.getDefaultMessage()));
            details.add(item);
        }
        return details;
    }

    private static ErrorCode errorCodeOf(HttpStatus status) {
        switch (status) {
            case BAD_REQUEST:
            case UNPROCESSABLE_ENTITY:
                return ErrorCode.INVALID_PARAM;
            case UNAUTHORIZED:
                return ErrorCode.UNAUTHORIZED;
            case FORBIDDEN:
                return ErrorCode.ACCESS_DENIED;
            case NOT_FOUND:
                return ErrorCode.NOT_FOUND;
            case CONFLICT:
                return ErrorCode.CONFLICT;
            case GONE:
                return ErrorCode.RESOURCE_GONE;
            case REQUEST_TIMEOUT:
                return ErrorCode.REQUEST_TIMEOUT;
            case TOO_MANY_REQUESTS:
                return ErrorCode.RATE_LIMITED;
            case BAD_GATEWAY:
                return ErrorCode.THIRD_PARTY_ERROR;
            case SERVICE_UNAVAILABLE:
                return ErrorCode.SERVICE_UNAVAILABLE;
            default:
                return ErrorCode.INTERNAL_ERROR;
        }
    }

    /**
     * Spring Boot 2 exposes getRawStatusCode(), while Spring Boot 3 exposes
     * getStatusCode(). Reflection keeps this seed source-compatible with both
     * generations; the normal Boot 2 path remains JDK 8 compatible.
     */
    private static HttpStatus responseStatus(ResponseStatusException exception) {
        try {
            Method getStatusCode = exception.getClass().getMethod("getStatusCode");
            Object statusCode = getStatusCode.invoke(exception);
            Method value = statusCode.getClass().getMethod("value");
            return resolveStatus(((Number) value.invoke(statusCode)).intValue());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Spring Boot 2.x path below.
        }
        try {
            Method getRawStatusCode = exception.getClass().getMethod("getRawStatusCode");
            Object statusCode = getRawStatusCode.invoke(exception);
            return resolveStatus(((Number) statusCode).intValue());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private static HttpStatus resolveStatus(int statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode);
        return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
