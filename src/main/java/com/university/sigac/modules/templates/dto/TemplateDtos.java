package com.university.sigac.modules.templates.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class TemplateDtos {

    private TemplateDtos() {
    }

    /** content is only sent in the detail view, not in the list. */
    public record TemplateDto(Long id, String name, Long councilTypeId, String councilTypeName, Long facultyId,
                              String facultyName, int version, boolean active, String comment,
                              String createdBy, Instant createdAt, String content) {
    }

    public record SaveTemplateRequest(@NotNull Long councilTypeId, Long facultyId, @Size(max = 150) String name,
                                      @NotBlank @Size(max = 500000) String content,
                                      @Size(max = 300) String comment) {
    }

    public record TemplateContentRequest(@NotBlank @Size(max = 500000) String content) {
    }

    public record VariableDto(String name, String type, String description, String example) {
    }
}
