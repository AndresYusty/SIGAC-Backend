package com.university.sigac.dto;

import com.university.sigac.enums.DecisionOutcome;
import com.university.sigac.enums.SessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** Session DTOs: scheduling, agenda and live conduct. */
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

    public record AttendanceItem(@NotNull Long userId, boolean present) {
    }

    public record AttendanceRequest(@NotEmpty @Valid List<AttendanceItem> items) {
    }

    public record DecisionRequest(@NotNull DecisionOutcome outcome, @Min(0) int votesFor,
                                  @Min(0) int votesAgainst, @Min(0) int abstentions,
                                  @Size(max = 500) String changeReason) {
    }

    public record NoteRequest(@NotBlank @Size(max = 20000) String content) {
    }

    public record MiscItemRequest(@NotBlank @Size(max = 200) String title, @Size(max = 4000) String description) {
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

    public record QuorumDto(int members, int present, int required, int percentage, boolean reached) {
    }

    public record AttendeeDto(Long userId, String name, String role, boolean present) {
    }

    public record AttendanceDto(SessionStatus sessionStatus, QuorumDto quorum, List<AttendeeDto> attendees) {
    }
}
