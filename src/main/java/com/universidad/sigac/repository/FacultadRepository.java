package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Facultad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultadRepository extends JpaRepository<Facultad, Long> {

    List<Facultad> findBySedeIdOrderByNombre(Long sedeId);
}
