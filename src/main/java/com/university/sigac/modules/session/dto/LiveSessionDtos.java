package com.university.sigac.modules.session.dto;

import com.university.sigac.modules.requests.enums.DecisionOutcome;
import com.university.sigac.modules.requests.enums.SessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Live session DTOs: attendance, quorum, decisions, debate notes and miscellaneous items. */
public final class LiveSessionDtos {

    private LiveSessionDtos() {
    }

    // ---------- Requests ----------
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
    public record QuorumDto(int members, int present, int required, int percentage, boolean reached) {
    }

    public record AttendeeDto(Long userId, String name, String role, boolean present) {
    }

    public record AttendanceDto(SessionStatus sessionStatus, QuorumDto quorum, List<AttendeeDto> attendees) {
    }
}
