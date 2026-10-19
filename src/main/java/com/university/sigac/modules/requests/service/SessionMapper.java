package com.university.sigac.modules.requests.service;

import com.university.sigac.modules.requests.dto.SessionDtos.AgendaItemDto;
import com.university.sigac.modules.requests.dto.SessionDtos.GuestDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionSummaryDto;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.entity.SessionGuest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts session entities into DTOs. Must be used inside a transaction (navigates LAZY relations). */
public final class SessionMapper {

    private SessionMapper() {
    }

    public static SessionDto toDto(Session s) {
        return new SessionDto(s.getId(), s.getCouncilType().getId(), s.getCouncilType().getName(), s.kind(),
                s.getFaculty().getId(), s.getFaculty().getName(), s.getScheduledAt(), s.getLocation(), s.getStatus(),
                s.isAgendaClosed(), s.getActualStart(), s.getActualEnd(),
                s.getItems().stream().map(SessionMapper::toItem).toList(),
                s.getGuests().stream().map(SessionMapper::toGuest).toList());
    }

    public static SessionSummaryDto toSummary(Session s) {
        return new SessionSummaryDto(s.getId(), s.getCouncilType().getName(), s.kind(), s.getFaculty().getName(),
                s.getScheduledAt(), s.getLocation(), s.getStatus(), s.isAgendaClosed(), s.getItems().size());
    }

    public static AgendaItemDto toItem(AgendaItem p) {
        return new AgendaItemDto(p.getId(), p.getPosition(), p.getTitle(), p.getDescription(),
                p.getRequest() == null ? null : p.getRequest().getCode(), p.isMiscellaneous(), p.isUrgent(),
                p.getOutcome(), p.getVotesFor(), p.getVotesAgainst(), p.getAbstentions(), p.getDecidedBy(),
                p.getDebateNotes());
    }

    /** Guests as template variables ({name, affiliation}) for the notice and the minutes. */
    public static List<Map<String, Object>> toGuestsModel(Session s) {
        List<Map<String, Object>> guests = new ArrayList<>();
        for (SessionGuest g : s.getGuests()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", g.getName());
            m.put("affiliation", g.getAffiliation());
            guests.add(m);
        }
        return guests;
    }

    private static GuestDto toGuest(SessionGuest g) {
        return new GuestDto(g.getId(), g.getName(), g.getEmail(), g.getAffiliation());
    }
}
