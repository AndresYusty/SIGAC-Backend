package com.university.sigac.controller;

import com.university.sigac.dto.MinutesDtos.AuditLogDto;
import com.university.sigac.dto.MinutesDtos.DashboardDto;
import com.university.sigac.dto.MinutesDtos.MinutesFile;
import com.university.sigac.dto.MinutesDtos.RepositoryMinutesDto;
import com.university.sigac.dto.MinutesDtos.VerificationDto;
import com.university.sigac.service.AuditService;
import com.university.sigac.service.DashboardService;
import com.university.sigac.service.RepositoryService;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Digital repository of published minutes, audit log and dashboard. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RepositoryController {

    private static final String QUERY = "hasAnyRole('SECRETARY_1','SECRETARY_2','PRESIDENT','COUNCIL_MEMBER','ADMIN')";

    private final RepositoryService repository;
    private final AuditService audit;
    private final DashboardService dashboard;

    @GetMapping("/repository/minutes")
    @PreAuthorize(QUERY)
    public Page<RepositoryMinutesDto> search(@RequestParam(required = false) String serialNumber,
                                             @RequestParam(required = false) Long councilTypeId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false) Long campusId,
                                             @RequestParam(required = false) Long facultyId,
                                             @RequestParam(required = false) String text,
                                             @PageableDefault(size = 20, sort = "sessionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return repository.search(serialNumber, councilTypeId, from, to, campusId, facultyId, text, pageable);
    }

    @GetMapping("/repository/minutes/{id}/verify")
    @PreAuthorize(QUERY)
    public VerificationDto verify(@PathVariable Long id) {
        return repository.verify(id);
    }

    /** RF-39: built-in viewer (inline). The SHA-256 is also validated before delivering the file. */
    @GetMapping("/repository/minutes/{id}/viewer")
    @PreAuthorize(QUERY)
    public ResponseEntity<byte[]> viewer(@PathVariable Long id) {
        return pdf(repository.readVerified(id, "EVENT_DOCUMENT_VIEW"), false);
    }

    /** RF-38 / UC-07: download with prior SHA-256 validation. */
    @GetMapping("/repository/minutes/{id}/download")
    @PreAuthorize(QUERY)
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return pdf(repository.readVerified(id, "EVENT_DOCUMENT_DOWNLOAD"), true);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogDto> audit(@RequestParam(required = false) String event,
                                   @RequestParam(required = false) String user,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        return audit.search(event, user, from, to, page, size);
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return dashboard.get();
    }

    private ResponseEntity<byte[]> pdf(MinutesFile f, boolean asAttachment) {
        ContentDisposition cd = (asAttachment ? ContentDisposition.attachment() : ContentDisposition.inline()).filename(f.name()).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-SIGAC-SHA256", f.hash())
                .body(f.data());
    }
}
