package com.universidad.sigac.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Excepción de negocio con código HTTP asociado. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> detalles;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<String> detalles) {
        super(message);
        this.status = status;
        this.detalles = detalles == null ? List.of() : detalles;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetalles() {
        return detalles;
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

    public static ApiException unprocessable(String msg, List<String> detalles) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, msg, detalles);
    }

    public static ApiException payloadTooLarge(String msg) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, msg);
    }
}
