package com.university.sigac.dto;

import com.university.sigac.enums.MinutesStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class MinutesDtos {

    private MinutesDtos() {
    }

    /** htmlContent is only included in the detail view. */
    public record MinutesDto(Long id, String serialNumber, MinutesStatus status, Long sessionId, String councilType,
                             String faculty, LocalDate sessionDate, String rejectionNote, String signedBy,
                             Instant signedAt, String htmlContent) {
    }

    public record MinutesContentRequest(@NotBlank @Size(max = 1000000) String htmlContent) {
    }

    public record RejectionRequest(@NotBlank @Size(max = 2000) String note) {
    }

    // ---------- Digital repository ----------
    public record RepositoryMinutesDto(Long id, String serialNumber, String councilType, String university, String campus,
                                       String faculty, LocalDate sessionDate, String signedBy, Instant signedAt,
                                       String sha256Hash) {
    }

    public record VerificationDto(String serialNumber, String storedHash, String computedHash, boolean intact) {
    }

    public record MinutesFile(String name, byte[] data, String hash) {
    }

    public record AuditLogDto(Long id, String event, String userEmail, String ip, String entity, String entityId,
                              String detail, Instant occurredAt) {
    }

    public record DashboardDto(String user, java.util.Map<String, Object> indicators) {
    }
}
