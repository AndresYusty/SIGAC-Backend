package com.universidad.sigac.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req, ex.getDetalles());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", req, detalles);
    }

    /** Errores de binding en @ModelAttribute (formularios multipart). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBind(BindException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", req, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", req, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Cuerpo de la petición ilegible o con formato incorrecto", req, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "El documento excede el límite máximo permitido", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "La operación viola una restricción de integridad (registro duplicado o referenciado)", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse er) { // 404, 405, 415, parte multipart ausente, etc.
            HttpStatus st = HttpStatus.resolve(er.getStatusCode().value());
            if (st != null && st.is4xxClientError()) {
                String msg = st == HttpStatus.NOT_FOUND ? "Recurso no encontrado"
                        : st == HttpStatus.METHOD_NOT_ALLOWED ? "Método HTTP no permitido para este recurso"
                        : "Petición inválida: " + st.getReasonPhrase();
                return build(st, msg, req, List.of());
            }
        }
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String msg, HttpServletRequest req, List<String> detalles) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), msg, req.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(body);
    }
}
