package com.university.sigac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Configuration DTOs: institutional hierarchy, council types and global parameters. */
public final class ConfigDtos {

    private ConfigDtos() {
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

    public record CouncilTypeDto(Long id, String code, String name) {
    }

    public record CouncilTypeRequest(@NotBlank @Size(max = 10) String code, @NotBlank @Size(max = 100) String name) {
    }

    public record ParameterDto(String key, String value, String description) {
    }

    public record ParameterRequest(@NotBlank @Size(max = 500) String value) {
    }
}
