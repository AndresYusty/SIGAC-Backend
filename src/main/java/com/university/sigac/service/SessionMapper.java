package com.university.sigac.service;

import com.university.sigac.dto.SessionDtos.AgendaItemDto;
import com.university.sigac.dto.SessionDtos.SessionDto;
import com.university.sigac.dto.SessionDtos.SessionSummaryDto;
import com.university.sigac.entity.AgendaItem;
import com.university.sigac.entity.Session;

/** Converts session entities into DTOs. Must be used inside a transaction (navigates LAZY relations). */
final class SessionMapper {

    private SessionMapper() {
    }

    static SessionDto toDto(Session s) {
        return new SessionDto(s.getId(), s.getCouncilType().getId(), s.getCouncilType().getName(), s.getFaculty().getId(),
                s.getFaculty().getName(), s.getScheduledAt(), s.getLocation(), s.getStatus(), s.isAgendaClosed(),
                s.getActualStart(), s.getActualEnd(), s.getItems().stream().map(SessionMapper::toItem).toList());
    }

    static SessionSummaryDto toSummary(Session s) {
        return new SessionSummaryDto(s.getId(), s.getCouncilType().getName(), s.getFaculty().getName(),
                s.getScheduledAt(), s.getLocation(), s.getStatus(), s.isAgendaClosed(), s.getItems().size());
    }

    static AgendaItemDto toItem(AgendaItem p) {
        return new AgendaItemDto(p.getId(), p.getPosition(), p.getTitle(), p.getDescription(),
                p.getRequest() == null ? null : p.getRequest().getCode(), p.isMiscellaneous(), p.getOutcome(),
                p.getVotesFor(), p.getVotesAgainst(), p.getAbstentions(), p.getDecidedBy(), p.getDebateNotes());
    }
}
