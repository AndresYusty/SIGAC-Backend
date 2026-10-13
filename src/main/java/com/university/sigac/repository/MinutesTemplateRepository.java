package com.university.sigac.repository;

import com.university.sigac.entity.MinutesTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinutesTemplateRepository extends JpaRepository<MinutesTemplate, Long> {

    List<MinutesTemplate> findAllByOrderByIdDesc();

    List<MinutesTemplate> findByCouncilTypeIdOrderByVersionDesc(Long councilTypeId);

    // General template (no Faculty) or Faculty-specific template
    Optional<MinutesTemplate> findTopByCouncilTypeIdAndFacultyIsNullOrderByVersionDesc(Long councilTypeId);

    Optional<MinutesTemplate> findTopByCouncilTypeIdAndFacultyIdOrderByVersionDesc(Long councilTypeId, Long facultyId);

    List<MinutesTemplate> findByCouncilTypeIdAndFacultyIsNullAndActiveTrue(Long councilTypeId);

    List<MinutesTemplate> findByCouncilTypeIdAndFacultyIdAndActiveTrue(Long councilTypeId, Long facultyId);
}
