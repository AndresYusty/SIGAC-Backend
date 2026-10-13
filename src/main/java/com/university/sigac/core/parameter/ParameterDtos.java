package com.university.sigac.core.parameter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Global parameter DTOs. */
public final class ParameterDtos {

    private ParameterDtos() {
    }

    public record ParameterDto(String key, String value, String description) {
    }

    public record ParameterRequest(@NotBlank @Size(max = 500) String value) {
    }
}
