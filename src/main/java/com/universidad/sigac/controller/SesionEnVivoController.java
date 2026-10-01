package com.universidad.sigac.controller;

import com.universidad.sigac.dto.SesionDtos.AsistenciaDto;
import com.universidad.sigac.dto.SesionDtos.AsistenciaRequest;
import com.universidad.sigac.dto.SesionDtos.DecisionRequest;
import com.universidad.sigac.dto.SesionDtos.NotaRequest;
import com.universidad.sigac.dto.SesionDtos.PuntoDto;
import com.universidad.sigac.dto.SesionDtos.PuntoVarioRequest;
import com.universidad.sigac.dto.SesionDtos.SesionDto;
import com.universidad.sigac.service.SesionEnVivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Conducción de la sesión en vivo: las operaciones de escritura son de la Secretaría 1. */
@RestController
@RequestMapping("/api/v1/sesiones/{sesionId}")
@RequiredArgsConstructor
public class SesionEnVivoController {

    private static final String LECTURA = "hasAnyRole('SECRETARIA_1','PRESIDENTE','CONSEJERO','ADMIN')";

    private final SesionEnVivoService service;

    @GetMapping("/asistencia")
    @PreAuthorize(LECTURA)
    public AsistenciaDto asistencia(@PathVariable Long sesionId) {
        return service.asistencia(sesionId);
    }

    @PutMapping("/asistencia")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public AsistenciaDto registrarAsistencia(@PathVariable Long sesionId, @Valid @RequestBody AsistenciaRequest req) {
        return service.registrarAsistencia(sesionId, req);
    }

    @PostMapping("/iniciar")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto iniciar(@PathVariable Long sesionId) {
        return service.iniciar(sesionId);
    }

    @PostMapping("/suspender")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto suspender(@PathVariable Long sesionId) {
        return service.suspender(sesionId);
    }

    @PostMapping("/reanudar")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto reanudar(@PathVariable Long sesionId) {
        return service.reanudar(sesionId);
    }

    @PostMapping("/finalizar")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto finalizar(@PathVariable Long sesionId) {
        return service.finalizar(sesionId);
    }

    @PostMapping("/puntos/{puntoId}/decision")
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public PuntoDto decision(@PathVariable Long sesionId, @PathVariable Long puntoId, @Valid @RequestBody DecisionRequest req) {
        return service.registrarDecision(sesionId, puntoId, req);
    }

    @PostMapping("/puntos/{puntoId}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public PuntoDto nota(@PathVariable Long sesionId, @PathVariable Long puntoId, @Valid @RequestBody NotaRequest req) {
        return service.agregarNota(sesionId, puntoId, req);
    }

    @PostMapping("/puntos-varios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARIA_1')")
    public SesionDto puntoVario(@PathVariable Long sesionId, @Valid @RequestBody PuntoVarioRequest req) {
        return service.agregarPuntoVario(sesionId, req);
    }
}
