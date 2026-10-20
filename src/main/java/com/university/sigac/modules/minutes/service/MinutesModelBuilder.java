package com.university.sigac.modules.minutes.service;

import com.university.sigac.core.util.DateUtil;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.service.SessionMapper;
import com.university.sigac.modules.session.dto.LiveSessionDtos.QuorumDto;
import com.university.sigac.modules.session.service.LiveSessionService;
import com.university.sigac.modules.users.enums.RoleName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the dictionary of official catalog variables (serialNumber, sessionDate, attendees, agreements...)
 * from what was recorded in the session. This is what Thymeleaf injects into the minutes template (RF-27).
 */
@Component
@RequiredArgsConstructor
public class MinutesModelBuilder {

    private final LiveSessionService liveSessionService;

    /** Session items that have no recorded decision. */
    public List<String> pendingItems(Session s) {
        return s.getItems().stream().filter(p -> !p.hasDecision()).map(p -> p.getPosition() + ". " + p.getTitle()).toList();
    }

    public Map<String, Object> build(Session s, String serialNumber) {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime reference = s.getActualStart() != null ? s.getActualStart() : s.getScheduledAt();
        m.put("serialNumber", serialNumber);
        m.put("sessionDate", DateUtil.longDate(reference.toLocalDate()));
        m.put("startTime", DateUtil.time(s.getActualStart()));
        m.put("endTime", DateUtil.time(s.getActualEnd()));
        m.put("location", s.documentLocation());
        m.put("councilType", s.getCouncilType().getName());
        m.put("university", s.getFaculty().getCampus().getUniversity().getName());
        m.put("campus", s.getFaculty().getCampus().getName());
        m.put("faculty", s.getFaculty().getName());

        QuorumDto q = liveSessionService.computeQuorum(s);
        m.put("quorum", quorumSummary(q));

        List<Map<String, Object>> attendees = new ArrayList<>();
        liveSessionService.attendees(s).forEach(attendee -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("name", attendee.name());
            a.put("role", attendee.role() == RoleName.ROLE_PRESIDENT ? "Presidente" : "Consejero");
            a.put("present", attendee.present());
            attendees.add(a);
        });
        m.put("attendees", attendees);
        m.put("guests", SessionMapper.toGuestsModel(s));
        m.put("hasGuests", !s.getGuests().isEmpty());

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
        return m;
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
