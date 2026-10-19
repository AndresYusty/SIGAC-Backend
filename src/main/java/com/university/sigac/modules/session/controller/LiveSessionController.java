package com.university.sigac.modules.session.controller;

import com.university.sigac.modules.requests.dto.SessionDtos.AgendaItemDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionDto;
import com.university.sigac.modules.session.dto.LiveSessionDtos.AttendanceDto;
import com.university.sigac.modules.session.dto.LiveSessionDtos.AttendanceRequest;
import com.university.sigac.modules.session.dto.LiveSessionDtos.DecisionRequest;
import com.university.sigac.modules.session.dto.LiveSessionDtos.MiscItemRequest;
import com.university.sigac.modules.session.dto.LiveSessionDtos.NoteRequest;
import com.university.sigac.modules.session.service.LiveSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Live session conduct: write operations belong to Council Secretary. */
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
@RequiredArgsConstructor
public class LiveSessionController {

    private static final String READ = "hasAnyRole('COUNCIL_SECRETARY','PRESIDENT','ADMIN')";

    private final LiveSessionService service;

    @GetMapping("/attendance")
    @PreAuthorize(READ)
    public AttendanceDto attendance(@PathVariable Long sessionId) {
        return service.attendance(sessionId);
    }

    @PutMapping("/attendance")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public AttendanceDto recordAttendance(@PathVariable Long sessionId, @Valid @RequestBody AttendanceRequest req) {
        return service.recordAttendance(sessionId, req);
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto start(@PathVariable Long sessionId) {
        return service.start(sessionId);
    }

    @PostMapping("/suspend")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto suspend(@PathVariable Long sessionId) {
        return service.suspend(sessionId);
    }

    @PostMapping("/resume")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto resume(@PathVariable Long sessionId) {
        return service.resume(sessionId);
    }

    @PostMapping("/finish")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto finish(@PathVariable Long sessionId) {
        return service.finish(sessionId);
    }

    @PostMapping("/items/{itemId}/decision")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public AgendaItemDto decision(@PathVariable Long sessionId, @PathVariable Long itemId, @Valid @RequestBody DecisionRequest req) {
        return service.recordDecision(sessionId, itemId, req);
    }

    @PostMapping("/items/{itemId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public AgendaItemDto note(@PathVariable Long sessionId, @PathVariable Long itemId, @Valid @RequestBody NoteRequest req) {
        return service.addNote(sessionId, itemId, req);
    }

    @PostMapping("/misc-items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto miscItem(@PathVariable Long sessionId, @Valid @RequestBody MiscItemRequest req) {
        return service.addMiscItem(sessionId, req);
    }
}
