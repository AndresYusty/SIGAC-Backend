package com.universidad.sigac.controller;

import com.universidad.sigac.dto.PlantillaDtos.ContenidoRequest;
import com.universidad.sigac.dto.PlantillaDtos.GuardarPlantillaRequest;
import com.universidad.sigac.dto.PlantillaDtos.PlantillaDto;
import com.universidad.sigac.dto.PlantillaDtos.VariableDto;
import com.universidad.sigac.service.PlantillaService;
import com.universidad.sigac.util.VariablesCatalogo;
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

/** RN-05: acceso exclusivo del perfil técnico (ROLE_DEV). */
@RestController
@RequestMapping("/api/v1/plantillas")
@PreAuthorize("hasRole('DEV')")
@RequiredArgsConstructor
public class PlantillaController {

    private final PlantillaService service;

    @GetMapping
    public List<PlantillaDto> listar(@RequestParam(required = false) Long tipoConsejoId) {
        return service.listar(tipoConsejoId);
    }

    /** Catálogo oficial de variables que se pueden usar en las plantillas (RF-11). */
    @GetMapping("/variables")
    public List<VariableDto> variables() {
        return VariablesCatalogo.VARIABLES;
    }

    @GetMapping("/{id}")
    public PlantillaDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Devuelve la lista de errores (vacía si la plantilla es válida). */
    @PostMapping("/validar")
    public List<String> validar(@Valid @RequestBody ContenidoRequest req) {
        return service.validar(req.contenido());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlantillaDto guardar(@Valid @RequestBody GuardarPlantillaRequest req) {
        return service.guardar(req);
    }

    @PutMapping("/{id}/activar")
    public PlantillaDto activar(@PathVariable Long id) {
        return service.activar(id);
    }

    /** RF-12: PDF de ejemplo con datos de prueba, sin guardar la plantilla. */
    @PostMapping("/previsualizar")
    public ResponseEntity<byte[]> previsualizar(@Valid @RequestBody ContenidoRequest req) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("previsualizacion.pdf").build().toString())
                .body(service.previsualizar(req.contenido()));
    }
}
