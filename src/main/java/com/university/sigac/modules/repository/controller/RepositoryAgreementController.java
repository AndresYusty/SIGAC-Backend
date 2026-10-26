package com.university.sigac.modules.repository.controller;

import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementSummaryDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementTypeDto;
import com.university.sigac.modules.agreements.service.AgreementService;
import com.university.sigac.modules.agreements.service.AgreementService.AgreementFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

/** Agreements available for consultation in the digital repository. */
@RestController
@RequestMapping("/api/v1/repository/agreements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COUNCIL_SECRETARY','FACULTY_SECRETARY','PRESIDENT','COUNCIL_MEMBER','ADMIN')")
public class RepositoryAgreementController {

    private final AgreementService agreements;

    @GetMapping("/types")
    public List<AgreementTypeDto> types() {
        return agreements.types();
    }

    @GetMapping
    public Page<AgreementSummaryDto> list(@RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) String typeCode,
                                          @RequestParam(required = false) String text,
                                          @PageableDefault(size = 20, sort = {"year", "number"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return agreements.list(year, typeCode, text, pageable);
    }

    @GetMapping("/{id}/viewer")
    public ResponseEntity<byte[]> viewer(@PathVariable Long id) {
        return pdf(agreements.open(id, "EVENT_DOCUMENT_VIEW"), false);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return pdf(agreements.open(id, "EVENT_DOCUMENT_DOWNLOAD"), true);
    }

    private static ResponseEntity<byte[]> pdf(AgreementFile file, boolean attachment) {
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(file.data());
    }
}
