package com.universidad.sigac.controller;

import com.universidad.sigac.dto.ConfigDtos.ParametroDto;
import com.universidad.sigac.dto.ConfigDtos.ParametroRequest;
import com.universidad.sigac.dto.ConfigDtos.TipoConsejoDto;
import com.universidad.sigac.dto.ConfigDtos.TipoConsejoRequest;
import com.universidad.sigac.service.ParametroService;
import com.universidad.sigac.service.TipoConsejoService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de tipos de consejo (RF-08) y parámetros globales del sistema (RF-09). */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final TipoConsejoService tiposConsejo;
    private final ParametroService parametros;

    @GetMapping("/tipos-consejo")
    public List<TipoConsejoDto> listarTiposConsejo() {
        return tiposConsejo.listar();
    }

    @PostMapping("/tipos-consejo")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TipoConsejoDto crearTipoConsejo(@Valid @RequestBody TipoConsejoRequest r) {
        return tiposConsejo.crear(r);
    }

    @PutMapping("/tipos-consejo/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TipoConsejoDto editarTipoConsejo(@PathVariable Long id, @Valid @RequestBody TipoConsejoRequest r) {
        return tiposConsejo.editar(id, r);
    }

    @GetMapping("/parametros")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ParametroDto> listarParametros() {
        return parametros.listar();
    }

    @PutMapping("/parametros/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    public ParametroDto actualizarParametro(@PathVariable String clave, @Valid @RequestBody ParametroRequest r) {
        return parametros.actualizar(clave, r.valor());
    }
}
