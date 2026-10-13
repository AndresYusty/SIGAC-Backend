package com.university.sigac.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Council type catalog DTOs. */
public final class CouncilTypeDtos {

    private CouncilTypeDtos() {
    }

    public record CouncilTypeDto(Long id, String code, String name) {
    }

    public record CouncilTypeRequest(@NotBlank @Size(max = 10) String code, @NotBlank @Size(max = 100) String name) {
    }
}
