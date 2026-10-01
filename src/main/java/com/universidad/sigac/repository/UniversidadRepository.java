package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Universidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversidadRepository extends JpaRepository<Universidad, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
