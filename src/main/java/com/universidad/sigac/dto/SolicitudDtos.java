package com.universidad.sigac.dto;

import com.universidad.sigac.enums.EstadoSolicitud;
import com.universidad.sigac.enums.TipoSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class SolicitudDtos {

    private SolicitudDtos() {
    }

    public record SolicitudRequest(@NotBlank @Size(max = 200) String titulo,
                                   @NotBlank @Size(max = 4000) String descripcion,
                                   @NotBlank @Size(max = 150) String solicitante,
                                   @NotNull TipoSolicitud tipoSolicitud,
                                   Long programaId) {
    }

    public record AnexoDto(Long id, String nombre, long tamanoBytes, String hashSha256) {
    }

    public record SolicitudDto(Long id, String codigo, String titulo, String descripcion, String solicitante,
                               TipoSolicitud tipoSolicitud, EstadoSolicitud estado, Long facultadId, String facultad,
                               Long programaId, String programa, String radicador, Instant fechaRadicacion,
                               List<AnexoDto> anexos) {
    }

    public record HistorialDto(EstadoSolicitud estadoAnterior, EstadoSolicitud estadoNuevo, String motivo,
                               String usuario, Instant fecha) {
    }

    /** Se usa para devolver y para rechazar: el motivo es obligatorio (RN-10). */
    public record MotivoRequest(@NotBlank @Size(max = 1900) String motivo) {
    }

    public record Archivo(String nombre, byte[] datos) {
    }
}
