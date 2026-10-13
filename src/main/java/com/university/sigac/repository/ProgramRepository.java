package com.university.sigac.repository;

import com.university.sigac.entity.Program;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findByFacultyIdOrderByName(Long facultyId);
}
