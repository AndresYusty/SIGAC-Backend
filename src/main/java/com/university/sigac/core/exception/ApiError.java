package com.university.sigac.core.exception;

import java.time.Instant;
import java.util.List;

/** Standard API error body. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, List<String> details) {
}
