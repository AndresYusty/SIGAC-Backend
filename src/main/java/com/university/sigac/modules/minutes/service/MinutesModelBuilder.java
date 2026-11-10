package com.university.sigac.modules.minutes.service;

import com.university.sigac.core.util.DateUtil;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.CouncilRequest;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.entity.SessionGuest;
import com.university.sigac.modules.requests.entity.SessionSummon;
import com.university.sigac.modules.requests.enums.RequestBranch;
import com.university.sigac.modules.requests.service.SessionMapper;
import com.university.sigac.modules.session.dto.LiveSessionDtos.AttendeeDto;
import com.university.sigac.modules.session.dto.LiveSessionDtos.QuorumDto;
import com.university.sigac.modules.session.service.LiveSessionService;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.CouncilKind;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.service.UserLookupService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the dictionary of official catalog variables (serialNumber, sessionDate, attendees, agreements...)
 * from what was recorded in the session. This is what Thymeleaf injects into the minutes template (RF-27).
 */
@Component
@RequiredArgsConstructor
public class MinutesModelBuilder {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a", ES);

    private final LiveSessionService liveSessionService;
    private final UserLookupService users;

    /** Session items that have no recorded decision. */
    public List<String> pendingItems(Session s) {
        return s.getItems().stream().filter(p -> !p.hasDecision()).map(p -> p.getPosition() + ". " + p.getTitle()).toList();
    }

    public Map<String, Object> build(Session s, String serialNumber) {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime reference = s.getActualStart() != null ? s.getActualStart() : s.getScheduledAt();
        LocalDate day = reference.toLocalDate();
        LocalDate endDay = s.getActualEnd() != null ? s.getActualEnd().toLocalDate() : day;
        m.put("serialNumber", serialNumber);
        m.put("minutesNumber", minutesNumber(serialNumber));
        m.put("sessionDate", DateUtil.longDate(day));
        m.put("day", String.format("%02d", day.getDayOfMonth()));
        m.put("month", String.format("%02d", day.getMonthValue()));
        m.put("year", String.valueOf(day.getYear()));
        m.put("endDate", DateUtil.longDate(endDay));
        m.put("startTime", DateUtil.time(s.getActualStart()));
        m.put("endTime", DateUtil.time(s.getActualEnd()));
        m.put("installedAt", reference.format(CLOCK).replace('\u202F', ' ').replace('\u00A0', ' '));
        m.put("location", s.documentLocation());
        m.put("councilType", s.getCouncilType().getName());
        m.put("sessionKind", kindLabel(s.kind()));
        m.put("university", s.getFaculty().getCampus().getUniversity().getName());
        m.put("campus", s.getFaculty().getCampus().getName());
        m.put("faculty", s.getFaculty().getName());

        QuorumDto q = liveSessionService.computeQuorum(s);
        m.put("quorum", quorumSummary(q));
        m.put("quorumReached", q.reached());
        m.put("quorumRequired", q.required());
        m.put("votingMembers", q.members());

        List<User> seated = liveSessionService.seated(s);
        Map<Long, Boolean> present = liveSessionService.attendees(s).stream()
                .collect(Collectors.toMap(AttendeeDto::userId, AttendeeDto::present, (a, b) -> a));
        List<Map<String, Object>> attendees = new ArrayList<>();
        List<String> presentPeople = new ArrayList<>();
        for (User person : seated) {
            boolean here = present.getOrDefault(person.getId(), false);
            String position = position(person, s);
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("number", attendees.size() + 1);
            a.put("name", person.getName());
            a.put("role", position);
            a.put("present", here);
            attendees.add(a);
            if (here) {
                presentPeople.add(person.getName() + ", " + lowerFirst(position));
            }
        }
        m.put("attendees", attendees);
        m.put("presentList", joinSpanish(presentPeople));

        List<Map<String, Object>> guests = new ArrayList<>();
        for (SessionGuest g : s.getGuests()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("number", attendees.size() + guests.size() + 1);
            row.put("name", g.getName());
            row.put("affiliation", g.getAffiliation() == null || g.getAffiliation().isBlank() ? "Invitado" : g.getAffiliation());
            guests.add(row);
        }
        m.put("guests", guests);
        m.put("hasGuests", !guests.isEmpty());

        User secretary = secretary(s);
        m.put("secretaryName", secretary == null ? "" : secretary.getName());
        m.put("secretaryRole", "Secretaría académica");
        m.put("presidentName", seated.stream().filter(u -> u.hasRole(RoleName.ROLE_PRESIDENT)).map(User::getName)
                .findFirst().orElse(""));

        List<Map<String, Object>> agenda = new ArrayList<>();
        List<Map<String, Object>> agreements = new ArrayList<>();
        for (AgendaItem p : s.getItems()) {
            String description = p.getDescription() == null || p.getDescription().isBlank() ? null : p.getDescription();
            if (!p.isMiscellaneous()) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("number", p.getPosition());
                o.put("title", p.getTitle());
                o.put("description", description);
                o.put("urgent", p.isUrgent());
                agenda.add(o);
            }
            Map<String, Object> ag = new LinkedHashMap<>();
            ag.put("number", p.getPosition());
            ag.put("title", p.getTitle());
            ag.put("description", description);
            ag.put("decision", p.hasDecision() ? p.getOutcome().label() : "Sin decisión");
            ag.put("votesFor", p.getVotesFor());
            ag.put("votesAgainst", p.getVotesAgainst());
            ag.put("abstentions", p.getAbstentions());
            ag.put("notes", p.getDebateNotes() == null ? "" : p.getDebateNotes()); // already sanitized XHTML
            ag.put("miscellaneous", p.isMiscellaneous());
            ag.put("urgent", p.isUrgent());
            agreements.add(ag);
        }
        m.put("agenda", agenda);
        m.put("agreements", agreements);

        List<AgendaItem> ordered = s.getItems().stream().sorted(Comparator.comparingInt(AgendaItem::getPosition)).toList();
        List<Map<String, Object>> sections = sections(ordered.stream().filter(p -> !p.isMiscellaneous()).toList());
        List<Map<String, Object>> varios = new ArrayList<>();
        for (AgendaItem p : ordered) {
            if (p.isMiscellaneous()) {
                varios.add(point(String.valueOf(varios.size() + 1), p));
            }
        }
        m.put("sections", sections);
        m.put("hasSections", !sections.isEmpty());
        m.put("varios", varios);
        m.put("hasVarios", !varios.isEmpty());
        return m;
    }

    // ---------------------------------------------------------------- agenda in the EV-CAL-FO-05 layout

    /** Point 3 of the agenda: requests grouped by the program (or area) they come from, in agenda order. */
    private static List<Map<String, Object>> sections(List<AgendaItem> items) {
        Map<String, List<AgendaItem>> groups = new LinkedHashMap<>();
        for (AgendaItem p : items) {
            groups.computeIfAbsent(groupOf(p), k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> sections = new ArrayList<>();
        int g = 0;
        for (Map.Entry<String, List<AgendaItem>> group : groups.entrySet()) {
            g++;
            List<Map<String, Object>> points = new ArrayList<>();
            int i = 0;
            for (AgendaItem p : group.getValue()) {
                points.add(point("3." + g + "." + (++i), p));
            }
            Map<String, Object> section = new LinkedHashMap<>();
            section.put("number", "3." + g);
            section.put("title", group.getKey());
            section.put("items", points);
            sections.add(section);
        }
        return sections;
    }

    private static Map<String, Object> point(String number, AgendaItem p) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("number", number);
        o.put("title", pointTitle(p));
        o.put("description", p.getDecisionDescription() == null || p.getDecisionDescription().isBlank()
                ? null : p.getDecisionDescription());
        o.put("decision", decision(p));
        o.put("notes", p.getDebateNotes() == null ? "" : p.getDebateNotes());
        return o;
    }

    private static String groupOf(AgendaItem p) {
        CouncilRequest r = p.getRequest();
        if (r == null) {
            return p.isUrgent() ? "Asuntos urgentes" : "Otros asuntos";
        }
        if (r.getProgram() != null) {
            return r.getProgram().getName();
        }
        return branchHeading(r.getRequestType() == null ? null : r.getRequestType().branch());
    }

    private static String branchHeading(RequestBranch branch) {
        if (branch == null) {
            return "Solicitudes";
        }
        return switch (branch) {
            case STUDENT -> "Solicitudes de estudiantes";
            case TEACHING -> "Solicitudes docentes";
            case CURRICULUM -> "Asuntos curriculares";
            case RESEARCH -> "Coordinación de investigación";
            case SOCIAL -> "Proyección social";
        };
    }

    /** "Requester — Subject" becomes "Subject de Requester cód. 2020..." as it is read in the minutes. */
    private static String pointTitle(AgendaItem p) {
        CouncilRequest r = p.getRequest();
        String title = p.getTitle();
        int dash = title.indexOf(" — ");
        if (r == null || dash < 0) {
            return title;
        }
        String text = title.substring(dash + 3) + " de " + title.substring(0, dash);
        return r.getStudentCode() == null || r.getStudentCode().isBlank() ? text : text + " cód. " + r.getStudentCode();
    }

    private static String decision(AgendaItem p) {
        if (!p.hasDecision()) {
            return "Sin decisión";
        }
        int inFavor = p.getVotesFor();
        int against = p.getVotesAgainst();
        int abstentions = p.getAbstentions();
        return switch (p.getOutcome()) {
            case APPROVED -> "APROBAR con " + votes(inFavor) + others(against, "en contra", abstentions);
            case DENIED -> "NEGAR con " + votes(against) + others(inFavor, "a favor", abstentions);
            case POSTPONED -> "APLAZAR" + tally(inFavor, against, abstentions);
            case CANCELLED -> "CANCELAR" + tally(inFavor, against, abstentions);
        };
    }

    private static String others(int opposite, String side, int abstentions) {
        StringBuilder text = new StringBuilder();
        if (opposite > 0) {
            text.append(", ").append(votes(opposite)).append(' ').append(side);
        }
        if (abstentions > 0) {
            text.append(", ").append(abstentions).append(abstentions == 1 ? " abstención" : " abstenciones");
        }
        return text.toString();
    }

    private static String tally(int inFavor, int against, int abstentions) {
        if (inFavor + against + abstentions == 0) {
            return "";
        }
        return " (" + votes(inFavor) + " a favor" + others(against, "en contra", abstentions) + ")";
    }

    private static String votes(int n) {
        return n + (n == 1 ? " voto" : " votos");
    }

    // ---------------------------------------------------------------- people

    /** Position printed next to each name: the dean of the faculty, the head of a program, or a counselor. */
    private static String position(User user, Session s) {
        if (user.hasRole(RoleName.ROLE_PRESIDENT)) {
            return "Decano " + s.getFaculty().getName();
        }
        if (user.hasRole(RoleName.ROLE_PROGRAM_HEAD) && user.getProgram() != null) {
            return "Jefe de programa " + user.getProgram().getName();
        }
        if (user.hasRole(RoleName.ROLE_PROGRAM_HEAD)) {
            return "Jefe de programa";
        }
        return "Consejero";
    }

    /** The council secretary summoned to the session or, for older sessions, the one of the faculty. */
    private User secretary(Session s) {
        Optional<User> summoned = s.getSummons().stream().map(SessionSummon::getUser)
                .filter(u -> SessionMapper.seat(u) == RoleName.ROLE_COUNCIL_SECRETARY)
                .findFirst();
        return summoned.orElseGet(() -> users.findActiveByFacultyAndRoles(s.getFaculty().getId(),
                List.of(RoleName.ROLE_COUNCIL_SECRETARY)).stream().findFirst().orElse(null));
    }

    // ---------------------------------------------------------------- text helpers

    /** MIN-CO-2026-015 is printed as acta No. 015. */
    private static String minutesNumber(String serialNumber) {
        int dash = serialNumber == null ? -1 : serialNumber.lastIndexOf('-');
        return dash < 0 ? String.valueOf(serialNumber) : serialNumber.substring(dash + 1);
    }

    private static String kindLabel(CouncilKind kind) {
        return switch (kind) {
            case ORDINARY -> "sesión ordinaria";
            case EXTRAORDINARY -> "sesión extraordinaria";
            case VIRTUAL -> "sesión virtual";
            case EXPANDED -> "sesión ampliada";
        };
    }

    private static String joinSpanish(List<String> parts) {
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return String.join("; ", parts.subList(0, parts.size() - 1)) + " y " + parts.get(parts.size() - 1);
    }

    private static String lowerFirst(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static String quorumSummary(QuorumDto q) {
        if (!q.applicable()) {
            return "Sesión virtual: no se exige quórum deliberatorio. Participaron " + q.present() + " de " + q.members()
                    + " integrantes con voto.";
        }
        return "Presentes " + q.present() + " de " + q.members() + " integrantes con voto (mínimo exigido: " + q.required()
                + "). " + (q.reached() ? "Se verificó el quórum deliberatorio." : "No se alcanzó el quórum.");
    }
}
