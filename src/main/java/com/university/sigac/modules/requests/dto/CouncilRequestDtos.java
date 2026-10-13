package com.university.sigac.modules.requests.dto;

import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class CouncilRequestDtos {

    private CouncilRequestDtos() {
    }

    public record CouncilRequestForm(@NotBlank @Size(max = 200) String title,
                                     @NotBlank @Size(max = 4000) String description,
                                     @NotBlank @Size(max = 150) String requester,
                                     @NotNull RequestType requestType,
                                     Long programId) {
    }

    public record AttachmentDto(Long id, String name, long sizeBytes, String sha256Hash) {
    }

    public record CouncilRequestDto(Long id, String code, String title, String description, String requester,
                                    RequestType requestType, RequestStatus status, Long facultyId, String faculty,
                                    Long programId, String program, String filedBy, Instant filedAt,
                                    List<AttachmentDto> attachments) {
    }

    public record HistoryDto(RequestStatus previousStatus, RequestStatus newStatus, String reason,
                             String user, Instant occurredAt) {
    }

    /** Used both to return and to reject: the reason is mandatory (RN-10). */
    public record ReasonRequest(@NotBlank @Size(max = 1900) String reason) {
    }

    public record FileContent(String name, byte[] data) {
    }
}
