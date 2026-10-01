package com.universidad.sigac.exception;

import java.time.Instant;
import java.util.List;

/** Cuerpo estándar de error de la API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, List<String> detalles) {
}
