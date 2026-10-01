package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Sesion;
import com.universidad.sigac.enums.EstadoSesion;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SesionRepository extends JpaRepository<Sesion, Long>, JpaSpecificationExecutor<Sesion> {

    long countByFacultadIdAndEstadoIn(Long facultadId, Collection<EstadoSesion> estados);
}
