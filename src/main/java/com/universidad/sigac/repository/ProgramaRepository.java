package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Programa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramaRepository extends JpaRepository<Programa, Long> {

    List<Programa> findByFacultadIdOrderByNombre(Long facultadId);
}
