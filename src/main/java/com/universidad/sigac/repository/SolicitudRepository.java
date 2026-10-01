package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Solicitud;
import com.universidad.sigac.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long>, JpaSpecificationExecutor<Solicitud> {

    long countByFacultadIdAndEstado(Long facultadId, EstadoSolicitud estado);

    long countByRadicadorIdAndEstado(Long radicadorId, EstadoSolicitud estado);
}
