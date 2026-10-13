package com.university.sigac.repository;

import com.university.sigac.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {

    boolean existsByNameIgnoreCase(String name);
}
