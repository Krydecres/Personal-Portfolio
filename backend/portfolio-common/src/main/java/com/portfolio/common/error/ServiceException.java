package com.portfolio.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base type for business errors raised by any service. The message is safe to show to clients:
 * never put stack traces, SQL or internal identifiers in it.
 */
public class ServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ServiceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static ServiceException notFound(String message) {
        return new ServiceException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ServiceException badRequest(String code, String message) {
        return new ServiceException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ServiceException conflict(String code, String message) {
        return new ServiceException(HttpStatus.CONFLICT, code, message);
    }

    public static ServiceException unauthorized(String code, String message) {
        return new ServiceException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ServiceException forbidden(String message) {
        return new ServiceException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ServiceException tooManyRequests(String message) {
        return new ServiceException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", message);
    }
}
