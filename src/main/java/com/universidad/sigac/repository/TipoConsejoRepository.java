package com.universidad.sigac.repository;

import com.universidad.sigac.entity.TipoConsejo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoConsejoRepository extends JpaRepository<TipoConsejo, Long> {

    Optional<TipoConsejo> findByCodigo(String codigo);
}
