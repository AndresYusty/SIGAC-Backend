package com.universidad.sigac.dto;

import com.universidad.sigac.enums.RolNombre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record UsuarioDto(Long id, String nombre, String email, boolean activo, List<String> roles,
                             Long facultadId, String facultadNombre, Long programaId, String programaNombre,
                             Instant ultimoAcceso) {
    }

    public record UsuarioCrearRequest(@NotBlank @Size(max = 150) String nombre,
                                      @NotBlank @Email @Size(max = 150) String email,
                                      @NotBlank @Size(min = 8, max = 100) String password,
                                      @NotEmpty Set<RolNombre> roles,
                                      Long facultadId, Long programaId) {
    }

    public record UsuarioActualizarRequest(@NotBlank @Size(max = 150) String nombre,
                                           @NotEmpty Set<RolNombre> roles,
                                           Long facultadId, Long programaId) {
    }

    public record CambioClaveRequest(@NotBlank String claveActual, @NotBlank @Size(min = 8, max = 100) String claveNueva) {
    }
}
