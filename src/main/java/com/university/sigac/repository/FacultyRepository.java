package com.university.sigac.repository;

import com.university.sigac.entity.Faculty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    List<Faculty> findByCampusIdOrderByName(Long campusId);
}
