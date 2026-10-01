package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Asistencia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findBySesionId(Long sesionId);

    Optional<Asistencia> findBySesionIdAndUsuarioId(Long sesionId, Long usuarioId);
}
