package com.university.sigac.modules.repository.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Digital repository, audit log and dashboard DTOs. */
public final class RepositoryDtos {

    private RepositoryDtos() {
    }

    public record RepositoryMinutesDto(Long id, String serialNumber, String councilType, String university, String campus,
                                       String faculty, LocalDate sessionDate, String signedBy, Instant signedAt,
                                       String sha256Hash) {
    }

    public record MinutesFile(String name, byte[] data, String hash) {
    }

    public record AuditLogDto(Long id, String event, String userEmail, String ip, String entity, String entityId,
                              String detail, Instant occurredAt) {
    }

    public record DashboardDto(String user, Map<String, Object> indicators) {
    }
}
