package com.universidad.sigac.controller;

import com.universidad.sigac.dto.ActaDtos.ActaDto;
import com.universidad.sigac.dto.ActaDtos.ContenidoRequest;
import com.universidad.sigac.dto.ActaDtos.ObservacionRequest;
import com.universidad.sigac.enums.EstadoActa;
import com.universidad.sigac.service.ActaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Generación, edición, aprobación y firma de actas (antes de su publicación en el repositorio). */
@RestController
@RequestMapping("/api/v1/actas")
@RequiredArgsConstructor
public class ActaController {

    private static final String REVISORES = "hasAnyRole('SECRETARIA_1','PRESIDENTE','ADMIN')";

    private final ActaService service;

    @PostMapping("/sesion/{sesionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public ActaDto generar(@PathVariable Long sesionId) {
        return service.compilar(sesionId);
    }

    @GetMapping
    @PreAuthorize(REVISORES)
    public Page<ActaDto> listar(@RequestParam(required = false) EstadoActa estado,
                                @PageableDefault(size = 20, sort = "creadoEn", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(estado, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(REVISORES)
    public ActaDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/vista-previa")
    @PreAuthorize(REVISORES)
    public ResponseEntity<byte[]> vistaPrevia(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("borrador-acta-" + id + ".pdf").build().toString())
                .body(service.vistaPrevia(id));
    }

    @PutMapping("/{id}/contenido")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public ActaDto editar(@PathVariable Long id, @Valid @RequestBody ContenidoRequest req) {
        return service.editar(id, req.contenidoHtml());
    }

    @PostMapping("/{id}/enviar-a-firma")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public ActaDto enviarAFirma(@PathVariable Long id) {
        return service.enviarAFirma(id);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('PRESIDENTE','SECRETARIA_1')")
    public ActaDto rechazar(@PathVariable Long id, @Valid @RequestBody ObservacionRequest req) {
        return service.rechazar(id, req.observacion());
    }

    @PostMapping("/{id}/aprobar-y-firmar")
    @PreAuthorize("hasAnyRole('PRESIDENTE','SECRETARIA_1')")
    public ActaDto aprobarYFirmar(@PathVariable Long id) {
        return service.aprobarYFirmar(id);
    }
}
