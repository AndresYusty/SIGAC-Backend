package com.universidad.sigac.controller;

import com.universidad.sigac.dto.ConfigDtos.FacultadRequest;
import com.universidad.sigac.dto.ConfigDtos.NodoDto;
import com.universidad.sigac.dto.ConfigDtos.ProgramaRequest;
import com.universidad.sigac.dto.ConfigDtos.SedeRequest;
import com.universidad.sigac.dto.ConfigDtos.UniversidadRequest;
import com.universidad.sigac.service.JerarquiaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/** Jerarquía Universidad > Sede > Facultad > Programa. Lectura: cualquier usuario autenticado. Escritura: ADMIN. */
@RestController
@RequestMapping("/api/v1/jerarquia")
@RequiredArgsConstructor
public class JerarquiaController {

    private final JerarquiaService service;

    @GetMapping("/universidades")
    public List<NodoDto> universidades() {
        return service.listarUniversidades();
    }

    @PostMapping("/universidades")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto crearUniversidad(@Valid @RequestBody UniversidadRequest r) {
        return service.crearUniversidad(r);
    }

    @PutMapping("/universidades/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto editarUniversidad(@PathVariable Long id, @Valid @RequestBody UniversidadRequest r) {
        return service.editarUniversidad(id, r);
    }

    @GetMapping("/sedes")
    public List<NodoDto> sedes(@RequestParam Long universidadId) {
        return service.listarSedes(universidadId);
    }

    @PostMapping("/sedes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto crearSede(@Valid @RequestBody SedeRequest r) {
        return service.crearSede(r);
    }

    @PutMapping("/sedes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto editarSede(@PathVariable Long id, @Valid @RequestBody SedeRequest r) {
        return service.editarSede(id, r);
    }

    @GetMapping("/facultades")
    public List<NodoDto> facultades(@RequestParam Long sedeId) {
        return service.listarFacultades(sedeId);
    }

    @PostMapping("/facultades")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto crearFacultad(@Valid @RequestBody FacultadRequest r) {
        return service.crearFacultad(r);
    }

    @PutMapping("/facultades/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto editarFacultad(@PathVariable Long id, @Valid @RequestBody FacultadRequest r) {
        return service.editarFacultad(id, r);
    }

    @GetMapping("/programas")
    public List<NodoDto> programas(@RequestParam Long facultadId) {
        return service.listarProgramas(facultadId);
    }

    @PostMapping("/programas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto crearPrograma(@Valid @RequestBody ProgramaRequest r) {
        return service.crearPrograma(r);
    }

    @PutMapping("/programas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodoDto editarPrograma(@PathVariable Long id, @Valid @RequestBody ProgramaRequest r) {
        return service.editarPrograma(id, r);
    }
}
