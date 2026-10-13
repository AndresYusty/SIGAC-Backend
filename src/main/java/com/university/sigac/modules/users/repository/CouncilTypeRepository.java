package com.university.sigac.modules.users.repository;

import com.university.sigac.modules.users.entity.CouncilType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouncilTypeRepository extends JpaRepository<CouncilType, Long> {

    Optional<CouncilType> findByCode(String code);
}
