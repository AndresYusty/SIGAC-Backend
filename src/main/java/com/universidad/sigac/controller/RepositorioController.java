package com.universidad.sigac.controller;

import com.universidad.sigac.dto.ActaDtos.ArchivoActa;
import com.universidad.sigac.dto.ActaDtos.AuditLogDto;
import com.universidad.sigac.dto.ActaDtos.DashboardDto;
import com.universidad.sigac.dto.ActaDtos.RepositorioActaDto;
import com.universidad.sigac.dto.ActaDtos.VerificacionDto;
import com.universidad.sigac.service.AuditService;
import com.universidad.sigac.service.DashboardService;
import com.universidad.sigac.service.RepositorioService;
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

/** Repositorio digital de actas publicadas, bitácora de auditoría y dashboard. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RepositorioController {

    private static final String CONSULTA = "hasAnyRole('SECRETARIA_1','SECRETARIA_2','PRESIDENTE','CONSEJERO','ADMIN')";

    private final RepositorioService repositorio;
    private final AuditService auditoria;
    private final DashboardService dashboard;

    @GetMapping("/repositorio/actas")
    @PreAuthorize(CONSULTA)
    public Page<RepositorioActaDto> buscar(@RequestParam(required = false) String consecutivo,
                                           @RequestParam(required = false) Long tipoConsejoId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) Long sedeId,
                                           @RequestParam(required = false) Long facultadId,
                                           @RequestParam(required = false) String texto,
                                           @PageableDefault(size = 20, sort = "fechaSesion", direction = Sort.Direction.DESC) Pageable pageable) {
        return repositorio.buscar(consecutivo, tipoConsejoId, desde, hasta, sedeId, facultadId, texto, pageable);
    }

    @GetMapping("/repositorio/actas/{id}/verificar")
    @PreAuthorize(CONSULTA)
    public VerificacionDto verificar(@PathVariable Long id) {
        return repositorio.verificar(id);
    }

    /** RF-39: visor integrado (inline). También valida el SHA-256 antes de entregar el archivo. */
    @GetMapping("/repositorio/actas/{id}/visor")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<byte[]> visor(@PathVariable Long id) {
        return pdf(repositorio.leerVerificado(id, "EVENT_DOCUMENT_VIEW"), false);
    }

    /** RF-38 / CU-07: descarga con validación SHA-256 previa. */
    @GetMapping("/repositorio/actas/{id}/descargar")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        return pdf(repositorio.leerVerificado(id, "EVENT_DOCUMENT_DOWNLOAD"), true);
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogDto> auditoria(@RequestParam(required = false) String evento,
                                       @RequestParam(required = false) String usuario,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        return auditoria.buscar(evento, usuario, desde, hasta, page, size);
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return dashboard.obtener();
    }

    private ResponseEntity<byte[]> pdf(ArchivoActa f, boolean adjunto) {
        ContentDisposition cd = (adjunto ? ContentDisposition.attachment() : ContentDisposition.inline()).filename(f.nombre()).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-SIGAC-SHA256", f.hash())
                .body(f.datos());
    }
}
