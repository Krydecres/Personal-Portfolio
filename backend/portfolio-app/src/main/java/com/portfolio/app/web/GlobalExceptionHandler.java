package com.portfolio.app.web;

import com.portfolio.common.error.ServiceException;
import com.portfolio.common.web.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps every failure to the {@link ApiError} contract. Clients get a safe message; details (stack
 * traces, SQL, class names) stay in the server log, correlated by an error id.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    ResponseEntity<ApiError> service(ServiceException e, HttpServletRequest req) {
        return body(e.getStatus(), e.getCode(), e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return validation(fields, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> invalidParams(HandlerMethodValidationException e, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getParameterValidationResults().forEach(r -> r.getResolvableErrors().forEach(err ->
                fields.putIfAbsent(r.getMethodParameter().getParameterName(), err.getDefaultMessage())));
        return validation(fields, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException e, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return validation(fields, req);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> unreadable(Exception e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Dữ liệu gửi lên không đúng định dạng", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> mediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Chỉ chấp nhận application/json", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException e, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy", req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimisticLock(OptimisticLockingFailureException e, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "Dữ liệu vừa được thay đổi ở nơi khác, hãy tải lại trang rồi thử lại", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Data integrity violation on {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "CONFLICT", "Dữ liệu bị trùng hoặc không hợp lệ", req);
    }

    /** Let Spring Security's ExceptionTranslationFilter produce the proper 401/403. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void security(RuntimeException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest req) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled error {} on {} {}", errorId, req.getMethod(), req.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Đã có lỗi xảy ra. Mã lỗi: " + errorId, req);
    }

    private static ResponseEntity<ApiError> validation(Map<String, String> fields, HttpServletRequest req) {
        String first = fields.values().stream().findFirst().orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_FAILED", first,
                req.getRequestURI(), fields));
    }

    private static ResponseEntity<ApiError> body(HttpStatus status, String code, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message, req.getRequestURI()));
    }
}
