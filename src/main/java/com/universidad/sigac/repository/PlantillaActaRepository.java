package com.universidad.sigac.repository;

import com.universidad.sigac.entity.PlantillaActa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaActaRepository extends JpaRepository<PlantillaActa, Long> {

    List<PlantillaActa> findAllByOrderByIdDesc();

    List<PlantillaActa> findByTipoConsejoIdOrderByVersionDesc(Long tipoConsejoId);

    // Plantilla general (sin Facultad) o específica de una Facultad
    Optional<PlantillaActa> findTopByTipoConsejoIdAndFacultadIsNullOrderByVersionDesc(Long tipoConsejoId);

    Optional<PlantillaActa> findTopByTipoConsejoIdAndFacultadIdOrderByVersionDesc(Long tipoConsejoId, Long facultadId);

    List<PlantillaActa> findByTipoConsejoIdAndFacultadIsNullAndActivaTrue(Long tipoConsejoId);

    List<PlantillaActa> findByTipoConsejoIdAndFacultadIdAndActivaTrue(Long tipoConsejoId, Long facultadId);
}
