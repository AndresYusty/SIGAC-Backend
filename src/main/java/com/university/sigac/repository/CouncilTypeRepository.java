package com.university.sigac.repository;

import com.university.sigac.entity.CouncilType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouncilTypeRepository extends JpaRepository<CouncilType, Long> {

    Optional<CouncilType> findByCode(String code);
}
