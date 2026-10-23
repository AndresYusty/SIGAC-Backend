package com.university.sigac.modules.requests.service;

import com.university.sigac.modules.requests.dto.SessionDtos.AgendaItemDto;
import com.university.sigac.modules.requests.dto.SessionDtos.GuestDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionSummaryDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SummonedDto;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.entity.SessionGuest;
import com.university.sigac.modules.requests.entity.SessionSummon;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import java.util.ArrayList;
import java.util.Comparator;
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
                s.getGuests().stream().map(SessionMapper::toGuest).toList(),
                toSummoned(s));
    }

    public static List<SummonedDto> toSummoned(Session s) {
        return s.getSummons().stream()
                .sorted(Comparator.comparingInt((SessionSummon sm) -> summonOrder(sm.getUser()))
                        .thenComparing(sm -> sm.getUser().getName()))
                .map(sm -> new SummonedDto(sm.getUser().getId(), sm.getUser().getName(), seat(sm.getUser())))
                .toList();
    }

    /** President, then counselors, then the council secretary. */
    public static int summonOrder(User user) {
        if (user.hasRole(RoleName.ROLE_PRESIDENT)) {
            return 0;
        }
        if (user.hasRole(RoleName.ROLE_COUNCIL_MEMBER)) {
            return 1;
        }
        return 2;
    }

    public static RoleName seat(User user) {
        if (user.hasRole(RoleName.ROLE_PRESIDENT)) {
            return RoleName.ROLE_PRESIDENT;
        }
        if (user.hasRole(RoleName.ROLE_COUNCIL_MEMBER)) {
            return RoleName.ROLE_COUNCIL_MEMBER;
        }
        return RoleName.ROLE_COUNCIL_SECRETARY;
    }

    public static String seatLabel(RoleName role) {
        return switch (role) {
            case ROLE_PRESIDENT -> "Presidente";
            case ROLE_COUNCIL_MEMBER -> "Consejero";
            case ROLE_COUNCIL_SECRETARY -> "Secretaría del Consejo";
            default -> role.name();
        };
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
