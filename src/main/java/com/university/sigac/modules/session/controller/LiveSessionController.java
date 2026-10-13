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

/** Live session conduct: write operations belong to Secretary 1. */
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
@RequiredArgsConstructor
public class LiveSessionController {

    private static final String READ = "hasAnyRole('SECRETARY_1','PRESIDENT','COUNCIL_MEMBER','ADMIN')";

    private final LiveSessionService service;

    @GetMapping("/attendance")
    @PreAuthorize(READ)
    public AttendanceDto attendance(@PathVariable Long sessionId) {
        return service.attendance(sessionId);
    }

    @PutMapping("/attendance")
    @PreAuthorize("hasRole('SECRETARY_1')")
    public AttendanceDto recordAttendance(@PathVariable Long sessionId, @Valid @RequestBody AttendanceRequest req) {
        return service.recordAttendance(sessionId, req);
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('SECRETARY_1')")
    public SessionDto start(@PathVariable Long sessionId) {
        return service.start(sessionId);
    }

    @PostMapping("/suspend")
    @PreAuthorize("hasRole('SECRETARY_1')")
    public SessionDto suspend(@PathVariable Long sessionId) {
        return service.suspend(sessionId);
    }

    @PostMapping("/resume")
    @PreAuthorize("hasRole('SECRETARY_1')")
    public SessionDto resume(@PathVariable Long sessionId) {
        return service.resume(sessionId);
    }

    @PostMapping("/finish")
    @PreAuthorize("hasRole('SECRETARY_1')")
    public SessionDto finish(@PathVariable Long sessionId) {
        return service.finish(sessionId);
    }

    @PostMapping("/items/{itemId}/decision")
    @PreAuthorize("hasRole('SECRETARY_1')")
    public AgendaItemDto decision(@PathVariable Long sessionId, @PathVariable Long itemId, @Valid @RequestBody DecisionRequest req) {
        return service.recordDecision(sessionId, itemId, req);
    }

    @PostMapping("/items/{itemId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARY_1')")
    public AgendaItemDto note(@PathVariable Long sessionId, @PathVariable Long itemId, @Valid @RequestBody NoteRequest req) {
        return service.addNote(sessionId, itemId, req);
    }

    @PostMapping("/misc-items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARY_1')")
    public SessionDto miscItem(@PathVariable Long sessionId, @Valid @RequestBody MiscItemRequest req) {
        return service.addMiscItem(sessionId, req);
    }
}
