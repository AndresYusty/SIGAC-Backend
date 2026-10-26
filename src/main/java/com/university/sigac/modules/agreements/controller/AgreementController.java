package com.university.sigac.modules.agreements.controller;

import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementDefaultsDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementRequest;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementSummaryDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementTypeDto;
import com.university.sigac.modules.agreements.service.AgreementService;
import com.university.sigac.modules.agreements.service.AgreementService.AgreementFile;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

/** The council secretary drafts agreements; the president can review and download them. */
@RestController
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private static final String WRITERS = "hasRole('COUNCIL_SECRETARY')";
    private static final String EDITORS = "hasAnyRole('COUNCIL_SECRETARY','ADMIN')";
    private static final String READERS = "hasAnyRole('COUNCIL_SECRETARY','PRESIDENT','ADMIN')";

    private final AgreementService service;

    @GetMapping("/types")
    @PreAuthorize(READERS)
    public List<AgreementTypeDto> types() {
        return service.types();
    }

    @GetMapping("/defaults")
    @PreAuthorize(WRITERS)
    public AgreementDefaultsDto defaults(@RequestParam(required = false) Long facultyId) {
        return service.defaults(facultyId);
    }

    @GetMapping
    @PreAuthorize(READERS)
    public Page<AgreementSummaryDto> list(@RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) String typeCode,
                                          @RequestParam(required = false) String text,
                                          @PageableDefault(size = 20, sort = {"year", "number"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(year, typeCode, text, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(READERS)
    public AgreementDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITERS)
    public AgreementDto create(@Valid @RequestBody AgreementRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(EDITORS)
    public AgreementDto update(@PathVariable Long id, @Valid @RequestBody AgreementRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/preview")
    @PreAuthorize(WRITERS)
    public ResponseEntity<byte[]> preview(@Valid @RequestBody AgreementRequest req) {
        return pdf(new AgreementFile("preview.pdf", service.preview(req)), false);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize(READERS)
    public ResponseEntity<byte[]> download(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean inline) {
        return pdf(service.pdf(id), !inline);
    }

    private static ResponseEntity<byte[]> pdf(AgreementFile file, boolean attachment) {
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(file.data());
    }
}
