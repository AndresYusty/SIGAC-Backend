package com.university.sigac.modules.requests.dto;

import com.university.sigac.modules.requests.enums.DecisionOutcome;
import com.university.sigac.modules.requests.enums.SessionStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** Session DTOs: scheduling and agenda. Live conduct DTOs live in the session module. */
public final class SessionDtos {

    private SessionDtos() {
    }

    // ---------- Requests ----------
    public record SessionRequest(@NotNull Long councilTypeId, @NotNull @Future LocalDateTime scheduledAt,
                                 @NotBlank @Size(max = 200) String location) {
    }

    public record AddItemRequest(@NotNull Long requestId) {
    }

    public record ReorderRequest(@NotEmpty List<Long> itemIds) {
    }

    // ---------- Responses ----------
    public record SessionSummaryDto(Long id, String councilType, String faculty, LocalDateTime scheduledAt,
                                    String location, SessionStatus status, boolean agendaClosed, int totalItems) {
    }

    public record AgendaItemDto(Long id, int position, String title, String description, String requestCode,
                                boolean miscellaneous, DecisionOutcome outcome, int votesFor, int votesAgainst,
                                int abstentions, String decidedBy, String debateNotes) {
    }

    /** Detail: session + agenda with the decisions and notes already recorded. */
    public record SessionDto(Long id, Long councilTypeId, String councilType, Long facultyId, String faculty,
                             LocalDateTime scheduledAt, String location, SessionStatus status, boolean agendaClosed,
                             LocalDateTime actualStart, LocalDateTime actualEnd, List<AgendaItemDto> items) {
    }
}
