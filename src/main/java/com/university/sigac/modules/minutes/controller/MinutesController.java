package com.university.sigac.modules.minutes.controller;

import com.university.sigac.modules.minutes.dto.MinutesDtos.MinutesContentRequest;
import com.university.sigac.modules.minutes.dto.MinutesDtos.MinutesDto;
import com.university.sigac.modules.minutes.dto.MinutesDtos.RejectionRequest;
import com.university.sigac.modules.minutes.enums.MinutesStatus;
import com.university.sigac.modules.minutes.service.MinutesService;
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

/** Generation, editing, approval and signature of minutes (before they are published in the repository). */
@RestController
@RequestMapping("/api/v1/minutes")
@RequiredArgsConstructor
public class MinutesController {

    private static final String REVIEWERS = "hasAnyRole('COUNCIL_SECRETARY','PRESIDENT','ADMIN')";

    private final MinutesService service;

    @PostMapping("/session/{sessionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public MinutesDto generate(@PathVariable Long sessionId) {
        return service.compile(sessionId);
    }

    @GetMapping
    @PreAuthorize(REVIEWERS)
    public Page<MinutesDto> list(@RequestParam(required = false) MinutesStatus status,
                                 @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(REVIEWERS)
    public MinutesDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize(REVIEWERS)
    public ResponseEntity<byte[]> preview(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("draft-minutes-" + id + ".pdf").build().toString())
                .body(service.preview(id));
    }

    @PutMapping("/{id}/content")
    @PreAuthorize("hasAnyRole('COUNCIL_SECRETARY','ADMIN')")
    public MinutesDto edit(@PathVariable Long id, @Valid @RequestBody MinutesContentRequest req) {
        return service.edit(id, req.htmlContent());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/send-to-signature")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public MinutesDto sendToSignature(@PathVariable Long id) {
        return service.sendToSignature(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('PRESIDENT','COUNCIL_SECRETARY')")
    public MinutesDto reject(@PathVariable Long id, @Valid @RequestBody RejectionRequest req) {
        return service.reject(id, req.note());
    }

    @PostMapping("/{id}/approve-and-sign")
    @PreAuthorize("hasAnyRole('PRESIDENT','COUNCIL_SECRETARY')")
    public MinutesDto approveAndSign(@PathVariable Long id) {
        return service.approveAndSign(id);
    }
}
