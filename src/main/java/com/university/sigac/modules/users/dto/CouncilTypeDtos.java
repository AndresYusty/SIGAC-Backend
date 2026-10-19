package com.university.sigac.modules.users.dto;

import com.university.sigac.modules.users.enums.CouncilKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Council type catalog DTOs. */
public final class CouncilTypeDtos {

    private CouncilTypeDtos() {
    }

    public record CouncilTypeDto(Long id, String code, String name, CouncilKind kind) {
    }

    public record CouncilTypeRequest(@NotBlank @Size(max = 100) String name) {
    }
}
