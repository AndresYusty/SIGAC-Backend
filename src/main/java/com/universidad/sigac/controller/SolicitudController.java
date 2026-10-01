package com.universidad.sigac.controller;

import com.universidad.sigac.dto.SolicitudDtos.Archivo;
import com.universidad.sigac.dto.SolicitudDtos.HistorialDto;
import com.universidad.sigac.dto.SolicitudDtos.MotivoRequest;
import com.universidad.sigac.dto.SolicitudDtos.SolicitudDto;
import com.universidad.sigac.dto.SolicitudDtos.SolicitudRequest;
import com.universidad.sigac.enums.EstadoSolicitud;
import com.universidad.sigac.service.SolicitudService;
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
public class SolicitudController {

    private static final String SECRETARIAS = "hasAnyRole('SECRETARIA_1','SECRETARIA_2','ADMIN')";

    private final SolicitudService service;

    /** CU-03: multipart/form-data con los campos de la solicitud y los PDF en la parte "anexos". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARIA_2')")
    public SolicitudDto radicar(@Valid @ModelAttribute SolicitudRequest datos,
                                @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) {
        return service.radicar(datos, anexos);
    }

    /** Corrige una solicitud RADICADA/DEVUELTA; puede agregar nuevos anexos. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SECRETARIA_2')")
    public SolicitudDto actualizar(@PathVariable Long id, @Valid @ModelAttribute SolicitudRequest datos,
                                   @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) {
        return service.actualizar(id, datos, anexos);
    }

    @GetMapping
    @PreAuthorize(SECRETARIAS)
    public Page<SolicitudDto> buscar(@RequestParam(required = false) EstadoSolicitud estado,
                                     @RequestParam(required = false) String codigo,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @PageableDefault(size = 20, sort = "fechaRadicacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.buscar(estado, codigo, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SECRETARIAS)
    public SolicitudDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize(SECRETARIAS)
    public List<HistorialDto> historial(@PathVariable Long id) {
        return service.historial(id);
    }

    @GetMapping("/{id}/anexos/{anexoId}/descargar")
    @PreAuthorize(SECRETARIAS)
    public ResponseEntity<byte[]> descargarAnexo(@PathVariable Long id, @PathVariable Long anexoId) {
        Archivo f = service.descargarAnexo(id, anexoId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(f.nombre()).build().toString())
                .body(f.datos());
    }

    // ---------- Evaluación por la Secretaría 1 ----------

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SolicitudDto aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SolicitudDto rechazar(@PathVariable Long id, @Valid @RequestBody MotivoRequest req) {
        return service.rechazar(id, req.motivo());
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SolicitudDto devolver(@PathVariable Long id, @Valid @RequestBody MotivoRequest req) {
        return service.devolver(id, req.motivo());
    }
}
