package com.university.sigac.modules.requests.dto;

import com.university.sigac.modules.requests.enums.DecisionOutcome;
import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.users.enums.CouncilKind;
import com.university.sigac.modules.users.enums.RoleName;
import jakarta.validation.constraints.Email;
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

    /** The location is required except for virtual sessions. memberIds are the people summoned to this session. */
    public record SessionRequest(@NotNull Long councilTypeId, @NotNull @Future LocalDateTime scheduledAt,
                                 @Size(max = 200) String location, @NotEmpty List<Long> memberIds) {
    }

    /** Admin correction of a session that may already have taken place. */
    public record SessionCorrection(@NotNull Long councilTypeId, @NotNull LocalDateTime scheduledAt,
                                    @Size(max = 200) String location) {
    }

    public record AddItemRequest(@NotNull Long requestId) {
    }

    public record UrgentItemRequest(@NotBlank @Size(max = 200) String title, @Size(max = 4000) String description) {
    }

    public record ReorderRequest(@NotEmpty List<Long> itemIds) {
    }

    public record GuestRequest(@NotBlank @Size(max = 150) String name, @Email @Size(max = 150) String email,
                               @Size(max = 150) String affiliation) {
    }

    // ---------- Responses ----------

    public record SessionSummaryDto(Long id, String councilType, CouncilKind councilKind, String faculty,
                                    LocalDateTime scheduledAt, String location, SessionStatus status,
                                    boolean agendaClosed, int totalItems) {
    }

    public record AgendaItemDto(Long id, int position, String title, String description, String requestCode,
                                boolean miscellaneous, boolean urgent, DecisionOutcome outcome, int votesFor,
                                int votesAgainst, int abstentions, String decidedBy, String debateNotes) {
    }

    public record GuestDto(Long id, String name, String email, String affiliation) {
    }

    /** A counselor or participant chosen to be summoned. */
    public record SummonedDto(Long userId, String name, RoleName role) {
    }

    /** Detail: session + agenda with the decisions and notes already recorded. */
    public record SessionDto(Long id, Long councilTypeId, String councilType, CouncilKind councilKind, Long facultyId,
                             String faculty, LocalDateTime scheduledAt, String location, SessionStatus status,
                             boolean agendaClosed, LocalDateTime actualStart, LocalDateTime actualEnd,
                             List<AgendaItemDto> items, List<GuestDto> guests, List<SummonedDto> summoned) {
    }
}
