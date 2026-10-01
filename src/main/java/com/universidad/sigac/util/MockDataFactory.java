package com.universidad.sigac.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datos de prueba (mock data) para la previsualización de plantillas (RF-12). */
public final class MockDataFactory {

    private MockDataFactory() {
    }

    public static Map<String, Object> crear() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("consecutivo", "ACTA-CF-2026-001");
        m.put("fechaSesion", "15 de agosto de 2026");
        m.put("horaInicio", "08:05");
        m.put("horaFin", "10:40");
        m.put("lugar", "Sala de Consejo - Bloque administrativo");
        m.put("tipoConsejo", "Consejo de Facultad");
        m.put("universidad", "Universidad Demo");
        m.put("sede", "Sede Principal");
        m.put("facultad", "Facultad de Ingeniería");
        m.put("quorum", "4 de 5 integrantes con voto (mínimo requerido: 3)");

        List<Map<String, Object>> asistentes = new ArrayList<>();
        asistentes.add(asistente("María Fernanda Gómez", "Presidente", true));
        asistentes.add(asistente("Carlos Andrés Rojas", "Consejero", true));
        asistentes.add(asistente("Luisa Martínez", "Consejero", true));
        asistentes.add(asistente("Jorge Pérez", "Consejero", true));
        asistentes.add(asistente("Ana Sofía Torres", "Consejero", false));
        m.put("listaAsistentes", asistentes);

        List<Map<String, Object>> orden = new ArrayList<>();
        orden.add(punto(1, "Aprobación de homologación de asignaturas", "Solicitud del programa de Ingeniería de Sistemas", "Aprobado", 4, 0, 0, "<p>Se discutió la pertinencia de la homologación.</p>", false));
        orden.add(punto(2, "Calendario de sustentaciones", null, "Aplazado", 2, 1, 1, "", false));
        m.put("ordenDelDia", orden);

        List<Map<String, Object>> acuerdos = new ArrayList<>(orden);
        acuerdos.add(punto(3, "Proposiciones y varios", null, "Aprobado", 4, 0, 0, "<p>Se solicita revisar horarios de laboratorio.</p>", true));
        m.put("acuerdos", acuerdos);
        return m;
    }

    private static Map<String, Object> asistente(String nombre, String rol, boolean presente) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("nombre", nombre);
        a.put("rol", rol);
        a.put("presente", presente);
        return a;
    }

    private static Map<String, Object> punto(int numero, String titulo, String descripcion, String decision, int favor,
                                             int contra, int abst, String notas, boolean varios) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("numero", numero);
        p.put("titulo", titulo);
        p.put("descripcion", descripcion);
        p.put("decision", decision);
        p.put("votosFavor", favor);
        p.put("votosContra", contra);
        p.put("abstenciones", abst);
        p.put("notas", notas);
        p.put("esVarios", varios);
        return p;
    }
}
