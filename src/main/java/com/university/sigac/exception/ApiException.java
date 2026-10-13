package com.university.sigac.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Business exception with an associated HTTP status code. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> details;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<String> details) {
        super(message);
        this.status = status;
        this.details = details == null ? List.of() : details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetails() {
        return details;
    }

    public static ApiException badRequest(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg);
    }

    public static ApiException unauthorized(String msg) {
        return new ApiException(HttpStatus.UNAUTHORIZED, msg);
    }

    public static ApiException forbidden(String msg) {
        return new ApiException(HttpStatus.FORBIDDEN, msg);
    }

    public static ApiException notFound(String msg) {
        return new ApiException(HttpStatus.NOT_FOUND, msg);
    }

    public static ApiException conflict(String msg) {
        return new ApiException(HttpStatus.CONFLICT, msg);
    }

    public static ApiException unprocessable(String msg, List<String> details) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, msg, details);
    }

    public static ApiException payloadTooLarge(String msg) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, msg);
    }
}
