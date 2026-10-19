package com.university.sigac.modules.requests.controller;

import com.university.sigac.modules.requests.dto.SessionDtos.AddItemRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.GuestRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.ReorderRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionSummaryDto;
import com.university.sigac.modules.requests.dto.SessionDtos.UrgentItemRequest;
import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.requests.service.AgendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Session scheduling and agenda building (Council Secretary). */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    /** Council members only see the minutes of the sessions they attended (repository module). */
    private static final String READ = "hasAnyRole('COUNCIL_SECRETARY','FACULTY_SECRETARY','PRESIDENT','ADMIN')";

    private final AgendaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto create(@Valid @RequestBody SessionRequest req) {
        return service.create(req);
    }

    @GetMapping
    @PreAuthorize(READ)
    public Page<SessionSummaryDto> list(@RequestParam(required = false) SessionStatus status,
                                        @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, pageable);
    }

    /** Session detail with its agenda, decisions and notes. */
    @GetMapping("/{id}")
    @PreAuthorize(READ)
    public SessionDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto addItem(@PathVariable Long id, @Valid @RequestBody AddItemRequest req) {
        return service.addItem(id, req.requestId());
    }

    /** Extraordinary Council only. */
    @PostMapping("/{id}/urgent-items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto addUrgentItem(@PathVariable Long id, @Valid @RequestBody UrgentItemRequest req) {
        return service.addUrgentItem(id, req);
    }

    /** Expanded Council only. */
    @PostMapping("/{id}/guests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto addGuest(@PathVariable Long id, @Valid @RequestBody GuestRequest req) {
        return service.addGuest(id, req);
    }

    @DeleteMapping("/{id}/guests/{guestId}")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto removeGuest(@PathVariable Long id, @PathVariable Long guestId) {
        return service.removeGuest(id, guestId);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return service.removeItem(id, itemId);
    }

    @PutMapping("/{id}/items/order")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto reorder(@PathVariable Long id, @Valid @RequestBody ReorderRequest req) {
        return service.reorder(id, req.itemIds());
    }

    /** Closes the agenda, generates the notice and sends it by email. */
    @PostMapping("/{id}/close-agenda")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public SessionDto closeAgenda(@PathVariable Long id) {
        return service.closeAgenda(id);
    }

    @GetMapping("/{id}/notice")
    @PreAuthorize(READ)
    public ResponseEntity<byte[]> notice(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("session-notice-" + id + ".pdf").build().toString())
                .body(service.downloadNotice(id));
    }
}
