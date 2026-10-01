package com.universidad.sigac.service;

import com.universidad.sigac.dto.ConfigDtos.TipoConsejoDto;
import com.universidad.sigac.dto.ConfigDtos.TipoConsejoRequest;
import com.universidad.sigac.entity.TipoConsejo;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.TipoConsejoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-08: catálogo de tipos de consejo (Facultad, Académico, Superior). */
@Service
@RequiredArgsConstructor
public class TipoConsejoService {

    private final TipoConsejoRepository repository;

    @Transactional(readOnly = true)
    public List<TipoConsejoDto> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional
    public TipoConsejoDto crear(TipoConsejoRequest r) {
        String codigo = r.codigo().trim().toUpperCase();
        if (repository.findByCodigo(codigo).isPresent()) {
            throw ApiException.conflict("Ya existe un tipo de consejo con ese código");
        }
        return aDto(repository.save(new TipoConsejo(codigo, r.nombre().trim())));
    }

    /** El código no se modifica: forma parte de los consecutivos ya emitidos (RN-13). */
    @Transactional
    public TipoConsejoDto editar(Long id, TipoConsejoRequest r) {
        TipoConsejo t = repository.findById(id).orElseThrow(() -> ApiException.notFound("Tipo de consejo no encontrado"));
        t.setNombre(r.nombre().trim());
        return aDto(t);
    }

    private TipoConsejoDto aDto(TipoConsejo t) {
        return new TipoConsejoDto(t.getId(), t.getCodigo(), t.getNombre());
    }
}
