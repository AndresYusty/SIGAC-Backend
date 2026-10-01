package com.universidad.sigac.dto;

import com.universidad.sigac.enums.EstadoActa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class ActaDtos {

    private ActaDtos() {
    }

    /** contenidoHtml solo se incluye en el detalle. */
    public record ActaDto(Long id, String consecutivo, EstadoActa estado, Long sesionId, String tipoConsejo,
                          String facultad, LocalDate fechaSesion, String observacionRechazo, String firmadoPor,
                          Instant firmadoEn, String contenidoHtml) {
    }

    public record ContenidoRequest(@NotBlank @Size(max = 1000000) String contenidoHtml) {
    }

    public record ObservacionRequest(@NotBlank @Size(max = 2000) String observacion) {
    }

    // ---------- Repositorio digital ----------
    public record RepositorioActaDto(Long id, String consecutivo, String tipoConsejo, String universidad, String sede,
                                     String facultad, LocalDate fechaSesion, String firmadoPor, Instant firmadoEn,
                                     String hashSha256) {
    }

    public record VerificacionDto(String consecutivo, String hashAlmacenado, String hashCalculado, boolean integro) {
    }

    public record ArchivoActa(String nombre, byte[] datos, String hash) {
    }

    public record AuditLogDto(Long id, String evento, String usuarioEmail, String ip, String entidad, String entidadId,
                              String detalle, Instant fecha) {
    }

    public record DashboardDto(String usuario, java.util.Map<String, Object> indicadores) {
    }
}
