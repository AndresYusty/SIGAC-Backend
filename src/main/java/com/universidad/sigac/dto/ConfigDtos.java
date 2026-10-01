package com.universidad.sigac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTOs de configuración: jerarquía institucional, tipos de consejo y parámetros globales. */
public final class ConfigDtos {

    private ConfigDtos() {
    }

    /** Nodo genérico de la jerarquía; padreId es el id del nivel inmediato superior. */
    public record NodoDto(Long id, String nombre, Long padreId, String ciudad) {
    }

    public record UniversidadRequest(@NotBlank @Size(max = 150) String nombre) {
    }

    public record SedeRequest(@NotBlank @Size(max = 150) String nombre, @Size(max = 100) String ciudad,
                              @NotNull Long universidadId) {
    }

    public record FacultadRequest(@NotBlank @Size(max = 150) String nombre, @NotNull Long sedeId) {
    }

    public record ProgramaRequest(@NotBlank @Size(max = 150) String nombre, @NotNull Long facultadId) {
    }

    public record TipoConsejoDto(Long id, String codigo, String nombre) {
    }

    public record TipoConsejoRequest(@NotBlank @Size(max = 10) String codigo, @NotBlank @Size(max = 100) String nombre) {
    }

    public record ParametroDto(String clave, String valor, String descripcion) {
    }

    public record ParametroRequest(@NotBlank @Size(max = 500) String valor) {
    }
}
