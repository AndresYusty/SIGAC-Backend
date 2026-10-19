package com.university.sigac.modules.repository.controller;

import com.university.sigac.modules.repository.dto.RepositoryDtos.MinutesFile;
import com.university.sigac.modules.repository.dto.RepositoryDtos.RepositoryMinutesDto;
import com.university.sigac.modules.repository.dto.RepositoryDtos.VerificationDto;
import com.university.sigac.modules.repository.service.RepositoryService;
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

/** Digital repository of published minutes. */
@RestController
@RequestMapping("/api/v1/repository/minutes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COUNCIL_SECRETARY','FACULTY_SECRETARY','PRESIDENT','COUNCIL_MEMBER','ADMIN')")
public class RepositoryController {

    private final RepositoryService repository;

    @GetMapping
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

    @GetMapping("/{id}/verify")
    public VerificationDto verify(@PathVariable Long id) {
        return repository.verify(id);
    }

    /** RF-39: built-in viewer (inline). The SHA-256 is also validated before delivering the file. */
    @GetMapping("/{id}/viewer")
    public ResponseEntity<byte[]> viewer(@PathVariable Long id) {
        return pdf(repository.readVerified(id, "EVENT_DOCUMENT_VIEW"), false);
    }

    /** RF-38 / UC-07: download with prior SHA-256 validation. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return pdf(repository.readVerified(id, "EVENT_DOCUMENT_DOWNLOAD"), true);
    }

    private ResponseEntity<byte[]> pdf(MinutesFile f, boolean asAttachment) {
        ContentDisposition cd = (asAttachment ? ContentDisposition.attachment() : ContentDisposition.inline()).filename(f.name()).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-SIGAC-SHA256", f.hash())
                .body(f.data());
    }
}
