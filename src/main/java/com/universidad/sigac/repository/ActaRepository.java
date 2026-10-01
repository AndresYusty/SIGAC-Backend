package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Acta;
import com.universidad.sigac.enums.EstadoActa;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActaRepository extends JpaRepository<Acta, Long>, JpaSpecificationExecutor<Acta> {

    boolean existsBySesionId(Long sesionId);

    long countByEstado(EstadoActa estado);

    long countByFacultadIdAndEstadoIn(Long facultadId, Collection<EstadoActa> estados);

    Page<Acta> findByEstado(EstadoActa estado, Pageable pageable);

    Page<Acta> findByFacultadId(Long facultadId, Pageable pageable);

    Page<Acta> findByFacultadIdAndEstado(Long facultadId, EstadoActa estado, Pageable pageable);

    List<Acta> findTop5ByFacultadIdAndEstadoOrderByFirmadoEnDesc(Long facultadId, EstadoActa estado);
}
