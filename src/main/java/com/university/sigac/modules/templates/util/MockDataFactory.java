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
        return m;
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
