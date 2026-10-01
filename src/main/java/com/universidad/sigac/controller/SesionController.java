package com.universidad.sigac.controller;

import com.universidad.sigac.dto.SesionDtos.AgregarPuntoRequest;
import com.universidad.sigac.dto.SesionDtos.ReordenarRequest;
import com.universidad.sigac.dto.SesionDtos.SesionDto;
import com.universidad.sigac.dto.SesionDtos.SesionRequest;
import com.universidad.sigac.dto.SesionDtos.SesionResumenDto;
import com.universidad.sigac.enums.EstadoSesion;
import com.universidad.sigac.service.AgendaService;
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

/** Programación de sesiones y construcción del Orden del Día (Secretaría 1). */
@RestController
@RequestMapping("/api/v1/sesiones")
@RequiredArgsConstructor
public class SesionController {

    private static final String LECTURA = "hasAnyRole('SECRETARIA_1','SECRETARIA_2','PRESIDENTE','CONSEJERO','ADMIN')";

    private final AgendaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto crear(@Valid @RequestBody SesionRequest req) {
        return service.crear(req);
    }

    @GetMapping
    @PreAuthorize(LECTURA)
    public Page<SesionResumenDto> listar(@RequestParam(required = false) EstadoSesion estado,
                                         @PageableDefault(size = 20, sort = "fechaProgramada", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(estado, pageable);
    }

    /** Detalle de la sesión con su Orden del Día, decisiones y notas. */
    @GetMapping("/{id}")
    @PreAuthorize(LECTURA)
    public SesionDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/puntos")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto agregarPunto(@PathVariable Long id, @Valid @RequestBody AgregarPuntoRequest req) {
        return service.agregarPunto(id, req.solicitudId());
    }

    @DeleteMapping("/{id}/puntos/{puntoId}")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto quitarPunto(@PathVariable Long id, @PathVariable Long puntoId) {
        return service.quitarPunto(id, puntoId);
    }

    @PutMapping("/{id}/puntos/orden")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto reordenar(@PathVariable Long id, @Valid @RequestBody ReordenarRequest req) {
        return service.reordenar(id, req.puntoIds());
    }

    /** Cierra el Orden del Día, genera la citación y la envía por correo. */
    @PostMapping("/{id}/cerrar-agenda")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto cerrarAgenda(@PathVariable Long id) {
        return service.cerrarAgenda(id);
    }

    @GetMapping("/{id}/citacion")
    @PreAuthorize(LECTURA)
    public ResponseEntity<byte[]> citacion(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("citacion-sesion-" + id + ".pdf").build().toString())
                .body(service.descargarCitacion(id));
    }
}
