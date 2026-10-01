package com.universidad.sigac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PlantillaDtos {

    private PlantillaDtos() {
    }

    /** contenido solo viaja en el detalle, no en el listado. */
    public record PlantillaDto(Long id, String nombre, Long tipoConsejoId, String tipoConsejoNombre, Long facultadId,
                               String facultadNombre, int version, boolean activa, String comentario,
                               String creadoPor, Instant creadoEn, String contenido) {
    }

    public record GuardarPlantillaRequest(@NotNull Long tipoConsejoId, Long facultadId, @Size(max = 150) String nombre,
                                          @NotBlank @Size(max = 500000) String contenido,
                                          @Size(max = 300) String comentario) {
    }

    public record ContenidoRequest(@NotBlank @Size(max = 500000) String contenido) {
    }

    public record VariableDto(String nombre, String tipo, String descripcion, String ejemplo) {
    }
}
