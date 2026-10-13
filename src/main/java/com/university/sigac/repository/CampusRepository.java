package com.university.sigac.repository;

import com.university.sigac.entity.Campus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, Long> {

    List<Campus> findByUniversityIdOrderByName(Long universityId);
}
