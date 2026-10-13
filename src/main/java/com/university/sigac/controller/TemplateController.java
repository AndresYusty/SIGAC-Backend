package com.university.sigac.controller;

import com.university.sigac.dto.TemplateDtos.SaveTemplateRequest;
import com.university.sigac.dto.TemplateDtos.TemplateContentRequest;
import com.university.sigac.dto.TemplateDtos.TemplateDto;
import com.university.sigac.dto.TemplateDtos.VariableDto;
import com.university.sigac.service.TemplateService;
import com.university.sigac.util.TemplateVariableCatalog;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RN-05: exclusive access for the technical profile (ROLE_DEV). */
@RestController
@RequestMapping("/api/v1/templates")
@PreAuthorize("hasRole('DEV')")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService service;

    @GetMapping
    public List<TemplateDto> list(@RequestParam(required = false) Long councilTypeId) {
        return service.list(councilTypeId);
    }

    /** Official catalog of variables that can be used in templates (RF-11). */
    @GetMapping("/variables")
    public List<VariableDto> variables() {
        return TemplateVariableCatalog.VARIABLES;
    }

    @GetMapping("/{id}")
    public TemplateDto get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Returns the list of errors (empty if the template is valid). */
    @PostMapping("/validate")
    public List<String> validate(@Valid @RequestBody TemplateContentRequest req) {
        return service.validate(req.content());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDto save(@Valid @RequestBody SaveTemplateRequest req) {
        return service.save(req);
    }

    @PutMapping("/{id}/activate")
    public TemplateDto activate(@PathVariable Long id) {
        return service.activate(id);
    }

    /** RF-12: sample PDF with mock data, without saving the template. */
    @PostMapping("/preview")
    public ResponseEntity<byte[]> preview(@Valid @RequestBody TemplateContentRequest req) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("preview.pdf").build().toString())
                .body(service.preview(req.content()));
    }
}
