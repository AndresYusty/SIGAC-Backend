package com.university.sigac.modules.users.repository;

import com.university.sigac.modules.users.entity.Program;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findByFacultyIdOrderByName(Long facultyId);

    boolean existsByFacultyId(Long facultyId);
}
