package com.universidad.sigac.service;

import com.universidad.sigac.entity.Consecutivo;
import com.universidad.sigac.repository.ConsecutivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Genera números consecutivos por clave (p. ej. "ACTA-CF-2026"), con bloqueo para evitar duplicados. */
@Service
@RequiredArgsConstructor
public class ConsecutivoService {

    private final ConsecutivoRepository repository;

    @Transactional
    public long siguiente(String clave) {
        Consecutivo c = repository.findByClaveForUpdate(clave)
                .orElseGet(() -> repository.saveAndFlush(new Consecutivo(clave, 0)));
        c.setUltimo(c.getUltimo() + 1);
        return c.getUltimo();
    }
}
