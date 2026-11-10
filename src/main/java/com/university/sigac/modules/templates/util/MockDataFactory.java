package com.university.sigac.modules.templates.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mock data for template previews (RF-12). */
public final class MockDataFactory {

    private MockDataFactory() {
    }

    public static Map<String, Object> create() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serialNumber", "MIN-CO-2026-001");
        m.put("minutesNumber", "001");
        m.put("sessionDate", "15 de agosto de 2026");
        m.put("day", "15");
        m.put("month", "08");
        m.put("year", "2026");
        m.put("endDate", "15 de agosto de 2026");
        m.put("startTime", "08:05");
        m.put("endTime", "10:40");
        m.put("installedAt", "08:05 a. m.");
        m.put("location", "Sala del Consejo - Edificio administrativo");
        m.put("councilType", "Consejo de Facultad");
        m.put("sessionKind", "sesión ordinaria");
        m.put("university", "Universidad Surcolombiana");
        m.put("campus", "Sede Neiva");
        m.put("faculty", "Facultad de Ingeniería");
        m.put("quorum", "Presentes 4 de 5 integrantes con voto (mínimo exigido: 3). Se verificó el quórum deliberatorio.");
        m.put("quorumReached", true);
        m.put("quorumRequired", 3);
        m.put("votingMembers", 5);

        List<Map<String, Object>> attendees = new ArrayList<>();
        attendees.add(attendee(1, "María Fernanda Gómez", "Decano Facultad de Ingeniería", true));
        attendees.add(attendee(2, "Carlos Andrés Rojas", "Consejero", true));
        attendees.add(attendee(3, "Luisa Martínez", "Consejero", true));
        attendees.add(attendee(4, "Jorge Pérez", "Jefe de programa Ingeniería de Software", true));
        attendees.add(attendee(5, "Ana Sofía Torres", "Consejero", false));
        m.put("attendees", attendees);
        m.put("presentList", "María Fernanda Gómez, decano Facultad de Ingeniería; Carlos Andrés Rojas, consejero; "
                + "Luisa Martínez, consejero y Jorge Pérez, jefe de programa Ingeniería de Software");
        List<Map<String, Object>> guests = new ArrayList<>();
        guests.add(Map.of("number", 6, "name", "Diana Ortiz", "affiliation", "Docente, Ingeniería de Software"));
        m.put("guests", guests);
        m.put("hasGuests", true);
        m.put("secretaryName", "Paula Andrea Vargas");
        m.put("secretaryRole", "Secretaría académica");
        m.put("presidentName", "María Fernanda Gómez");

        List<Map<String, Object>> agenda = new ArrayList<>();
        agenda.add(item(1, "Homologación de asignaturas", "Solicitud del programa de Ingeniería de Software", "Aprobado", 4, 0, 0,
                "<p>Se revisó la pertinencia de la homologación.</p>", false));
        agenda.add(item(2, "Calendario de sustentaciones", null, "Aplazado", 2, 1, 1, "", false));
        m.put("agenda", agenda);

        List<Map<String, Object>> agreements = new ArrayList<>(agenda);
        agreements.add(item(3, "Proposiciones y varios", null, "Aprobado", 4, 0, 0,
                "<p>Se solicita revisar los horarios de laboratorio.</p>", true));
        m.put("agreements", agreements);

        List<Map<String, Object>> software = List.of(
                point("3.1.1", "Homologación de asignaturas de Juan Camilo Ruiz cód. 20201234567",
                        "El estudiante cursó las asignaturas en otra institución acreditada.", "APROBAR con 4 votos",
                        "<p>Se revisó la pertinencia de la homologación.</p>"));
        List<Map<String, Object>> teaching = List.of(
                point("3.2.1", "Calendario de sustentaciones de Departamento de Ingeniería", null,
                        "APLAZAR (2 votos a favor, 1 voto en contra, 1 abstención)", ""));
        m.put("sections", List.of(section("3.1", "Ingeniería de Software", software),
                section("3.2", "Solicitudes docentes", teaching)));
        m.put("hasSections", true);
        m.put("varios", List.of(point("1", "Horarios de laboratorio", null, "APROBAR con 4 votos",
                "<p>Se solicita revisar los horarios de laboratorio.</p>")));
        m.put("hasVarios", true);
        return m;
    }

    private static Map<String, Object> attendee(int number, String name, String role, boolean present) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("number", number);
        a.put("name", name);
        a.put("role", role);
        a.put("present", present);
        return a;
    }

    private static Map<String, Object> section(String number, String title, List<Map<String, Object>> items) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("number", number);
        s.put("title", title);
        s.put("items", items);
        return s;
    }

    private static Map<String, Object> point(String number, String title, String description, String decision, String notes) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("number", number);
        p.put("title", title);
        p.put("description", description);
        p.put("decision", decision);
        p.put("notes", notes);
        return p;
    }

    private static Map<String, Object> item(int number, String title, String description, String decision, int votesFor,
                                            int votesAgainst, int abstentions, String notes, boolean miscellaneous) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("number", number);
        p.put("title", title);
        p.put("description", description);
        p.put("decision", decision);
        p.put("votesFor", votesFor);
        p.put("votesAgainst", votesAgainst);
        p.put("abstentions", abstentions);
        p.put("notes", notes);
        p.put("miscellaneous", miscellaneous);
        p.put("urgent", false);
        return p;
    }
}
