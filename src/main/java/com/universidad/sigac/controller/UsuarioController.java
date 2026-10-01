package com.universidad.sigac.controller;

import com.universidad.sigac.dto.AuthDtos.MensajeResponse;
import com.universidad.sigac.dto.UsuarioDtos.CambioClaveRequest;
import com.universidad.sigac.dto.UsuarioDtos.UsuarioActualizarRequest;
import com.universidad.sigac.dto.UsuarioDtos.UsuarioCrearRequest;
import com.universidad.sigac.dto.UsuarioDtos.UsuarioDto;
import com.universidad.sigac.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping("/me")
    public UsuarioDto miPerfil() {
        return service.perfilActual();
    }

    @PutMapping("/me/password")
    public MensajeResponse cambiarMiClave(@Valid @RequestBody CambioClaveRequest req) {
        service.cambiarClavePropia(req);
        return new MensajeResponse("Contraseña actualizada correctamente");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public Page<UsuarioDto> listar(@RequestParam(required = false) String q,
                                   @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return service.listar(q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UsuarioDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UsuarioDto crear(@Valid @RequestBody UsuarioCrearRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UsuarioDto cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        return service.cambiarEstado(id, activo);
    }
}
