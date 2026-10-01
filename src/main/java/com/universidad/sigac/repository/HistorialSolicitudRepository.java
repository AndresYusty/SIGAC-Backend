package com.universidad.sigac.repository;

import com.universidad.sigac.entity.HistorialSolicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialSolicitudRepository extends JpaRepository<HistorialSolicitud, Long> {

    List<HistorialSolicitud> findBySolicitudIdOrderByFechaAscIdAsc(Long solicitudId);
}
