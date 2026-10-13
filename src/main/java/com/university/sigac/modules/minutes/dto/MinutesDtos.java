package com.university.sigac.modules.minutes.dto;

import com.university.sigac.modules.minutes.enums.MinutesStatus;
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
}
