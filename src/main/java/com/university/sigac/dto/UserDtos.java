package com.university.sigac.dto;

import com.university.sigac.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(Long id, String name, String email, boolean active, List<String> roles,
                          Long facultyId, String facultyName, Long programId, String programName,
                          Instant lastLogin) {
    }

    public record UserCreateRequest(@NotBlank @Size(max = 150) String name,
                                    @NotBlank @Email @Size(max = 150) String email,
                                    @NotBlank @Size(min = 8, max = 100) String password,
                                    @NotEmpty Set<RoleName> roles,
                                    Long facultyId, Long programId) {
    }

    public record UserUpdateRequest(@NotBlank @Size(max = 150) String name,
                                    @NotEmpty Set<RoleName> roles,
                                    Long facultyId, Long programId) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 100) String newPassword) {
    }
}
