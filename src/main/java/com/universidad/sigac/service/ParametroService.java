package com.universidad.sigac.service;

import com.universidad.sigac.dto.ConfigDtos.ParametroDto;
import com.universidad.sigac.entity.Parametro;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.ParametroRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Parámetros globales (RN-04): se leen de la base de datos, así que se pueden cambiar sin redesplegar. */
@Service
@RequiredArgsConstructor
public class ParametroService {

    public static final String ARCHIVOS_MAX_MB = "archivos.max_mb";
    public static final String QUORUM_PORCENTAJE = "quorum.porcentaje";

    private final ParametroRepository repository;

    @Transactional(readOnly = true)
    public int getInt(String clave, int defecto) {
        return repository.findById(clave).map(p -> {
            try {
                return Integer.parseInt(p.getValor().trim());
            } catch (NumberFormatException e) {
                return defecto;
            }
        }).orElse(defecto);
    }

    @Transactional(readOnly = true)
    public List<ParametroDto> listar() {
        return repository.findAll().stream()
                .map(p -> new ParametroDto(p.getClave(), p.getValor(), p.getDescripcion())).toList();
    }

    @Transactional
    public ParametroDto actualizar(String clave, String valor) {
        Parametro p = repository.findById(clave).orElseThrow(() -> ApiException.notFound("Parámetro no encontrado: " + clave));
        int n;
        try {
            n = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("El valor debe ser un número entero");
        }
        if (n <= 0 || (QUORUM_PORCENTAJE.equals(clave) && n > 100)) {
            throw ApiException.badRequest("Valor fuera de rango para " + clave);
        }
        p.setValor(String.valueOf(n));
        return new ParametroDto(p.getClave(), p.getValor(), p.getDescripcion());
    }

    /** Crea el parámetro con su valor por defecto si aún no existe (carga inicial). */
    @Transactional
    public void asegurar(String clave, String valor, String descripcion) {
        if (!repository.existsById(clave)) {
            repository.save(new Parametro(clave, valor, descripcion));
        }
    }
}
