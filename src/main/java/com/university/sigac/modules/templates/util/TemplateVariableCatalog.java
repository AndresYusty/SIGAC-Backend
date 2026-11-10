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
                            + "'notes' is already sanitized XHTML: use th:utext=\"${item.notes}\"", "th:each=\"ag : ${agreements}\""),
            new VariableDto("minutesNumber", "text", "Consecutive number of the minutes taken from the serial", "015"),
            new VariableDto("day", "text", "Day of the session, two digits", "07"),
            new VariableDto("month", "text", "Month of the session, two digits", "09"),
            new VariableDto("year", "text", "Year of the session", "2026"),
            new VariableDto("endDate", "text", "Closing date in long format", "7 de septiembre de 2026"),
            new VariableDto("installedAt", "text", "Time the session was installed, 12-hour clock", "02:30 p. m."),
            new VariableDto("sessionKind", "text", "Kind of session in words", "sesión ordinaria"),
            new VariableDto("quorumReached", "boolean", "true when the deliberative quorum was verified",
                    "th:if=\"${quorumReached}\""),
            new VariableDto("quorumRequired", "number", "Minimum number of voting members for quorum", "5"),
            new VariableDto("votingMembers", "number", "Number of members with vote", "9"),
            new VariableDto("presentList", "text", "Members present with their position, joined in Spanish",
                    "Ana Pérez, decano; Luis Gómez, consejero y Marta Ruiz, consejero"),
            new VariableDto("secretaryName", "text", "Name of the council secretary", "Diana Ortiz"),
            new VariableDto("secretaryRole", "text", "Position of the council secretary", "Secretaría académica"),
            new VariableDto("presidentName", "text", "Name of the council president", "Ana Pérez"),
            new VariableDto("sections", "list",
                    "Requests grouped by program or area: iterable of {number, title, items}; each item has "
                            + "{number, title, description, decision, notes}", "th:each=\"s : ${sections}\""),
            new VariableDto("hasSections", "boolean", "true when there are requests on the agenda", "th:if=\"${hasSections}\""),
            new VariableDto("varios", "list", "Miscellaneous points: iterable of {number, title, description, decision, notes}",
                    "th:each=\"v : ${varios}\""),
            new VariableDto("hasVarios", "boolean", "true when miscellaneous points were added", "th:if=\"${hasVarios}\""));

    public static final Set<String> NAMES = VARIABLES.stream().map(VariableDto::name).collect(Collectors.toUnmodifiableSet());

    private TemplateVariableCatalog() {
    }
}
