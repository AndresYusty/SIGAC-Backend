package com.university.sigac.modules.repository.controller;

import com.university.sigac.modules.repository.dto.RepositoryDtos.AuditLogDto;
import com.university.sigac.modules.repository.service.AuditQueryService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Append-only audit log query. */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService audit;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogDto> search(@RequestParam(required = false) String event,
                                    @RequestParam(required = false) String user,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size) {
        return audit.search(event, user, from, to, page, size);
    }
}
