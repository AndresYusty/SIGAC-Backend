package com.university.sigac.modules.users.repository;

import com.university.sigac.modules.users.entity.Campus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, Long> {

    List<Campus> findByUniversityIdOrderByName(Long universityId);
}
