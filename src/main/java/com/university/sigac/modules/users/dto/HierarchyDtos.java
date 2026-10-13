package com.university.sigac.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Institutional hierarchy DTOs: University > Campus > Faculty > Program. */
public final class HierarchyDtos {

    private HierarchyDtos() {
    }

    /** Generic hierarchy node; parentId is the id of the level immediately above. */
    public record NodeDto(Long id, String name, Long parentId, String city) {
    }

    public record UniversityRequest(@NotBlank @Size(max = 150) String name) {
    }

    public record CampusRequest(@NotBlank @Size(max = 150) String name, @Size(max = 100) String city,
                                @NotNull Long universityId) {
    }

    public record FacultyRequest(@NotBlank @Size(max = 150) String name, @NotNull Long campusId) {
    }

    public record ProgramRequest(@NotBlank @Size(max = 150) String name, @NotNull Long facultyId) {
    }
}
