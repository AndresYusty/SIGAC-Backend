package com.university.sigac.modules.users.repository;

import com.university.sigac.modules.users.entity.Faculty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    List<Faculty> findByCampusIdOrderByName(Long campusId);

    boolean existsByCampusId(Long campusId);
}
