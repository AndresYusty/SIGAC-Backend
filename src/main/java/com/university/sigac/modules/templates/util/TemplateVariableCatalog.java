package com.university.sigac.modules.templates.util;

import com.university.sigac.modules.templates.dto.TemplateDtos.VariableDto;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Official catalog of standardized dynamic variables (RN-06 / RF-11). */
public final class TemplateVariableCatalog {

    public static final List<VariableDto> VARIABLES = List.of(
            new VariableDto("serialNumber", "text", "Unalterable serial number of the minutes", "MIN-CO-2026-004"),
            new VariableDto("sessionDate", "text", "Session date in long format", "August 15, 2026"),
            new VariableDto("startTime", "text", "Actual opening time of the session", "08:05"),
            new VariableDto("endTime", "text", "Actual closing time of the session", "10:40"),
            new VariableDto("location", "text", "Session location", "Council Room"),
            new VariableDto("councilType", "text", "Name of the council type", "Ordinary Council"),
            new VariableDto("university", "text", "Name of the university", "Demo University"),
            new VariableDto("campus", "text", "Name of the campus", "Main Campus"),
            new VariableDto("faculty", "text", "Name of the faculty", "Faculty of Engineering"),
            new VariableDto("quorum", "text", "Summary of the verified quorum", "6 of 9 members (minimum required: 5)"),
            new VariableDto("attendees", "list", "Iterable of {name, role, present}", "th:each=\"a : ${attendees}\""),
            new VariableDto("guests", "list", "Expanded Council guests (voice, no vote): iterable of {name, affiliation}",
                    "th:each=\"g : ${guests}\""),
            new VariableDto("hasGuests", "boolean", "true when the session has invited guests", "th:if=\"${hasGuests}\""),
            new VariableDto("agenda", "list", "Iterable of {number, title, description, urgent} (original agenda)", "th:each=\"o : ${agenda}\""),
            new VariableDto("agreements", "list",
                    "Iterable of {number, title, description, decision, votesFor, votesAgainst, abstentions, notes, miscellaneous, urgent}. "
                            + "'notes' is already sanitized XHTML: use th:utext=\"${item.notes}\"", "th:each=\"ag : ${agreements}\""));

    public static final Set<String> NAMES = VARIABLES.stream().map(VariableDto::name).collect(Collectors.toUnmodifiableSet());

    private TemplateVariableCatalog() {
    }
}
