package com.universidad.sigac.util;

import com.universidad.sigac.dto.PlantillaDtos.VariableDto;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Catálogo oficial de variables dinámicas estandarizadas (RN-06 / RF-11). */
public final class VariablesCatalogo {

    public static final List<VariableDto> VARIABLES = List.of(
            new VariableDto("consecutivo", "texto", "Número consecutivo inalterable del acta", "ACTA-CF-2026-004"),
            new VariableDto("fechaSesion", "texto", "Fecha de la sesión en formato largo", "15 de agosto de 2026"),
            new VariableDto("horaInicio", "texto", "Hora real de apertura de la sesión", "08:05"),
            new VariableDto("horaFin", "texto", "Hora real de cierre de la sesión", "10:40"),
            new VariableDto("lugar", "texto", "Lugar de la sesión", "Sala de Consejo"),
            new VariableDto("tipoConsejo", "texto", "Nombre del tipo de consejo", "Consejo de Facultad"),
            new VariableDto("universidad", "texto", "Nombre de la universidad", "Universidad Demo"),
            new VariableDto("sede", "texto", "Nombre de la sede", "Sede Principal"),
            new VariableDto("facultad", "texto", "Nombre de la facultad", "Facultad de Ingeniería"),
            new VariableDto("quorum", "texto", "Resumen del quórum verificado", "6 de 9 integrantes (mínimo requerido: 5)"),
            new VariableDto("listaAsistentes", "lista", "Iterable de {nombre, rol, presente}", "th:each=\"a : ${listaAsistentes}\""),
            new VariableDto("ordenDelDia", "lista", "Iterable de {numero, titulo, descripcion} (agenda original)", "th:each=\"o : ${ordenDelDia}\""),
            new VariableDto("acuerdos", "lista",
                    "Iterable de {numero, titulo, descripcion, decision, votosFavor, votosContra, abstenciones, notas, esVarios}. "
                            + "'notas' es XHTML ya sanitizado: use th:utext=\"${item.notas}\"", "th:each=\"ac : ${acuerdos}\""));

    public static final Set<String> NOMBRES = VARIABLES.stream().map(VariableDto::nombre).collect(Collectors.toUnmodifiableSet());

    private VariablesCatalogo() {
    }
}
