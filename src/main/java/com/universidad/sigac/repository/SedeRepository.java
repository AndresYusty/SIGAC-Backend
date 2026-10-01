package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Sede;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SedeRepository extends JpaRepository<Sede, Long> {

    List<Sede> findByUniversidadIdOrderByNombre(Long universidadId);
}
