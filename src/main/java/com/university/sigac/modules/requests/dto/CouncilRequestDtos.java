package com.university.sigac.modules.requests.dto;

import com.university.sigac.modules.requests.enums.CancellationKind;
import com.university.sigac.modules.requests.enums.RequestBranch;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.RequestType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CouncilRequestDtos {

    private CouncilRequestDtos() {
    }

    /** MI-FOR-028 fields. The title is derived from the topic; description is the justification. */
    public record CouncilRequestForm(@NotBlank @Size(max = 150) String requester,
                                     @NotNull RequestType requestType,
                                     Long programId,
                                     @NotNull LocalDate filedOn,
                                     @NotBlank @Size(max = 1) String academicPeriod,
                                     @Size(max = 30) String studentCode,
                                     @Size(max = 30) String identityDocument,
                                     @Size(max = 80) String identityIssuedIn,
                                     @NotBlank @Email @Size(max = 150) String email,
                                     @NotBlank @Size(max = 30) String phone,
                                     boolean electronicNotice,
                                     @Size(max = 150) String counselorName,
                                     @Size(max = 200) String otherDetail,
                                     CancellationKind cancellationKind,
                                     @NotBlank @Size(max = 4000) String justification,
                                     @Size(max = 20000) String coursesJson) {
    }

    public record CourseLine(Integer line, String courseName, String groupCode, String subgroup,
                             String courseNumber, String gradeNumber, String gradeWords,
                             Boolean attendedRegularly, Boolean failingCourse) {
    }

    public record AttachmentDto(Long id, String name, long sizeBytes, String sha256Hash, String documentCode) {
    }

    public record CouncilRequestDto(Long id, String code, String title, String description, String requester,
                                    RequestBranch branch, RequestType requestType, CancellationKind cancellationKind,
                                    RequestStatus status, Long facultyId, String faculty, String campus,
                                    Long programId, String program, String filedBy, Instant filedAt,
                                    LocalDate filedOn, String academicPeriod, String studentCode,
                                    String identityDocument, String identityIssuedIn, String email, String phone,
                                    boolean electronicNotice, String counselorName, String otherDetail,
                                    List<CourseLine> courses, List<AttachmentDto> attachments) {
    }

    public record TopicDto(String code, List<String> documents, boolean courses, boolean grades,
                           boolean otherDetail, boolean cancellationChoice) {
    }

    public record BranchDto(String code, List<TopicDto> topics) {
    }

    /** Catalog of branches and the filer's faculty, campus and program (MI-FOR-028 header). */
    public record CatalogDto(String faculty, String campus, String program, Long programId, List<BranchDto> branches) {
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
