package com.universidad.sigac.dto;

import com.universidad.sigac.enums.EstadoSesion;
import com.universidad.sigac.enums.ResultadoDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de sesiones: programación, Orden del Día y conducción en vivo. */
public final class SesionDtos {

    private SesionDtos() {
    }

    // ---------- Requests ----------
    public record SesionRequest(@NotNull Long tipoConsejoId, @NotNull @Future LocalDateTime fechaProgramada,
                                @NotBlank @Size(max = 200) String lugar) {
    }

    public record AgregarPuntoRequest(@NotNull Long solicitudId) {
    }

    public record ReordenarRequest(@NotEmpty List<Long> puntoIds) {
    }

    public record AsistenciaItem(@NotNull Long usuarioId, boolean presente) {
    }

    public record AsistenciaRequest(@NotEmpty @Valid List<AsistenciaItem> items) {
    }

    public record DecisionRequest(@NotNull ResultadoDecision resultado, @Min(0) int votosFavor,
                                  @Min(0) int votosContra, @Min(0) int abstenciones,
                                  @Size(max = 500) String motivoModificacion) {
    }

    public record NotaRequest(@NotBlank @Size(max = 20000) String contenido) {
    }

    public record PuntoVarioRequest(@NotBlank @Size(max = 200) String titulo, @Size(max = 4000) String descripcion) {
    }

    // ---------- Responses ----------
    public record SesionResumenDto(Long id, String tipoConsejo, String facultad, LocalDateTime fechaProgramada,
                                   String lugar, EstadoSesion estado, boolean agendaCerrada, int totalPuntos) {
    }

    public record PuntoDto(Long id, int orden, String titulo, String descripcion, String solicitudCodigo,
                           boolean esVarios, ResultadoDecision resultado, int votosFavor, int votosContra,
                           int abstenciones, String decididoPor, String notasDebate) {
    }

    /** Detalle: sesión + Orden del Día con las decisiones y notas ya registradas. */
    public record SesionDto(Long id, Long tipoConsejoId, String tipoConsejo, Long facultadId, String facultad,
                            LocalDateTime fechaProgramada, String lugar, EstadoSesion estado, boolean agendaCerrada,
                            LocalDateTime inicioReal, LocalDateTime finReal, List<PuntoDto> puntos) {
    }

    public record QuorumDto(int integrantes, int presentes, int requeridos, int porcentaje, boolean alcanzado) {
    }

    public record AsistenteDto(Long usuarioId, String nombre, String rol, boolean presente) {
    }

    public record AsistenciaDto(EstadoSesion estadoSesion, QuorumDto quorum, List<AsistenteDto> asistentes) {
    }
}
