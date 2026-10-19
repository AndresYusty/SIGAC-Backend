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
        m.put("sessionDate", "August 15, 2026");
        m.put("startTime", "08:05");
        m.put("endTime", "10:40");
        m.put("location", "Council Room - Administrative Building");
        m.put("councilType", "Ordinary Council");
        m.put("university", "Demo University");
        m.put("campus", "Main Campus");
        m.put("faculty", "Faculty of Engineering");
        m.put("quorum", "4 of 5 voting members (minimum required: 3)");

        List<Map<String, Object>> attendees = new ArrayList<>();
        attendees.add(attendee("Maria Fernanda Gomez", "President", true));
        attendees.add(attendee("Carlos Andres Rojas", "Council member", true));
        attendees.add(attendee("Luisa Martinez", "Council member", true));
        attendees.add(attendee("Jorge Perez", "Council member", true));
        attendees.add(attendee("Ana Sofia Torres", "Council member", false));
        m.put("attendees", attendees);
        List<Map<String, Object>> guests = new ArrayList<>();
        guests.add(Map.of("name", "Diana Ortiz", "affiliation", "Professor, Systems Engineering"));
        m.put("guests", guests);
        m.put("hasGuests", true);

        List<Map<String, Object>> agenda = new ArrayList<>();
        agenda.add(item(1, "Approval of course equivalences", "Request from the Systems Engineering program", "Approved", 4, 0, 0, "<p>The relevance of the equivalence was discussed.</p>", false));
        agenda.add(item(2, "Thesis defense schedule", null, "Postponed", 2, 1, 1, "", false));
        m.put("agenda", agenda);

        List<Map<String, Object>> agreements = new ArrayList<>(agenda);
        agreements.add(item(3, "Proposals and miscellaneous", null, "Approved", 4, 0, 0, "<p>A review of laboratory schedules is requested.</p>", true));
        m.put("agreements", agreements);
        agreement(m);
        return m;
    }

    /** Sample of Acuerdo 028 de 2026 (credit addition), the richer of the two reference documents. */
    private static void agreement(Map<String, Object> m) {
        m.put("agreementNumber", "028");
        m.put("agreementYear", "2026");
        m.put("agreementDate", "27 DE ENERO");
        m.put("agreementSubject", "Por medio del cual se resuelve una solicitud de adición de créditos académicos");
        m.put("councilTitle", "EL CONSEJO DE FACULTAD DE INGENIERÍA");
        m.put("issuedOn", "Dado en Neiva, a los veintisiete (27) días del mes de enero de dos mil veintiséis (2026).");
        m.put("presidentName", "MARLIO BEDOYA CARDOSO");
        m.put("secretaryName", "LUZ MARINA CAMPOS MURILLO");
        m.put("hasStudents", true);

        List<Map<String, Object>> students = new ArrayList<>();
        students.add(student("20251233689", "OSCAR JULIAN PEREZ MOREA", "1", "TEC. OBRAS CIVILES"));
        m.put("students", students);

        List<Map<String, Object>> recitals = new ArrayList<>();
        recitals.add(block("Que el Consejo Superior de las Universidad Surcolombiana mediante Acuerdo No. 046 "
                + "del 14 de diciembre de 2012 modificó el Acuerdo No. 027 de 2011;", false));
        recitals.add(block("Que, en ese sentido, el parágrafo 2° del artículo 1° del Acuerdo 046 de 2012, establece "
                + "que el número máximo de créditos que puede registrar el estudiante en un periodo académico será "
                + "hasta veinte (20), sin embargo, los estudiantes podrán solicitar ante el Consejo de Facultad "
                + "respectivo, en única instancia, cursar hasta veinticuatro (24) créditos en un periodo académico.", false));
        recitals.add(block("Que el estudiante relacionado a continuación, solicitó ampliación de créditos académicos "
                + "para el periodo 2026-1", true));
        recitals.add(block("Que el Consejo de Facultad de Ingeniería en consulta virtual del día 27 de enero de 2026, "
                + "según Acta No. 002 C.V. de la misma fecha, luego de analizar la solicitud presentada y consultar "
                + "la ficha académica del estudiante, resolvió dar APROBACION.", false));
        recitals.add(block("En mérito de lo expuesto, el Consejo de Facultad de Ingeniería,", false));
        m.put("recitals", recitals);

        List<Map<String, Object>> articles = new ArrayList<>();
        articles.add(article("1", "APROBAR con fundamento en lo expuesto en la parte motiva, la solicitud de 1 crédito "
                + "adicional a los 20 permitidos, según lo establecido en el artículo 1° del Acuerdo 027 de 2011, "
                + "modificado por el artículo 2° del Acuerdo 046 de 2012, para el periodo 2026-1 al estudiante "
                + "relacionado a continuación.", true));
        articles.add(article("2", "Notificar el contenido del presente Acto Administrativo al estudiante relacionado "
                + "en el artículo primero, informándole que contra el mismo proceden los recursos de Reposición y "
                + "Apelación, los cuales deberán ser presentados por escrito ante el Consejo de Facultad de Ingeniería "
                + "dentro de los diez (10) días hábiles siguientes, contados a partir del día hábil siguiente a la "
                + "fecha de su notificación.", false));
        articles.add(article("3", "Enviar copia del presente Acto Administrativo a la Oficina de Registro y Control "
                + "Académico, para su conocimiento y trámite respectivo.", false));
        articles.add(article("4", "El presente Acuerdo rige a partir de la fecha de su expedición.", false));
        m.put("articles", articles);
    }

    private static Map<String, Object> student(String code, String name, String credits, String program) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("code", code);
        s.put("name", name);
        s.put("credits", credits);
        s.put("program", program);
        return s;
    }

    private static Map<String, Object> block(String text, boolean showTable) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("text", text);
        b.put("showTable", showTable);
        return b;
    }

    private static Map<String, Object> article(String number, String text, boolean showTable) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("number", number);
        a.put("text", text);
        a.put("showTable", showTable);
        return a;
    }

    private static Map<String, Object> attendee(String name, String role, boolean present) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", name);
        a.put("role", role);
        a.put("present", present);
        return a;
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
