package com.university.sigac.modules.requests.controller;

import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestForm;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.FileContent;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.HistoryDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.ReasonRequest;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.service.CouncilRequestService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class CouncilRequestController {

    private static final String SECRETARIES = "hasAnyRole('COUNCIL_SECRETARY','FACULTY_SECRETARY','ADMIN')";

    private final CouncilRequestService service;

    /** UC-03: multipart/form-data with the request fields and the PDFs in the "attachments" part. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FACULTY_SECRETARY')")
    public CouncilRequestDto file(@Valid @ModelAttribute CouncilRequestForm form,
                                  @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments) {
        return service.file(form, attachments);
    }

    /** Corrects a FILED/RETURNED request; new attachments may be added. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FACULTY_SECRETARY')")
    public CouncilRequestDto update(@PathVariable Long id, @Valid @ModelAttribute CouncilRequestForm form,
                                    @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments) {
        return service.update(id, form, attachments);
    }

    @GetMapping
    @PreAuthorize(SECRETARIES)
    public Page<CouncilRequestDto> search(@RequestParam(required = false) RequestStatus status,
                                          @RequestParam(required = false) String code,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @PageableDefault(size = 20, sort = "filedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(status, code, from, to, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SECRETARIES)
    public CouncilRequestDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize(SECRETARIES)
    public List<HistoryDto> history(@PathVariable Long id) {
        return service.history(id);
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    @PreAuthorize(SECRETARIES)
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        FileContent f = service.downloadAttachment(id, attachmentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(f.name()).build().toString())
                .body(f.data());
    }

    // ---------- Review by Council Secretary ----------

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public CouncilRequestDto approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public CouncilRequestDto reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest req) {
        return service.reject(id, req.reason());
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('COUNCIL_SECRETARY')")
    public CouncilRequestDto returnRequest(@PathVariable Long id, @Valid @RequestBody ReasonRequest req) {
        return service.returnRequest(id, req.reason());
    }
}
