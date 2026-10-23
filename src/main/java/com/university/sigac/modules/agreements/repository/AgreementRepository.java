package com.university.sigac.modules.agreements.repository;

import com.university.sigac.modules.agreements.entity.Agreement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AgreementRepository extends JpaRepository<Agreement, Long>, JpaSpecificationExecutor<Agreement> {

    @Query("select coalesce(max(a.number), 0) from Agreement a where a.faculty.id = :facultyId and a.year = :year")
    int lastNumber(Long facultyId, int year);

    boolean existsByFacultyId(Long facultyId);

    boolean existsByFacultyIdAndYearAndNumberAndIdNot(Long facultyId, int year, int number, Long id);

    Optional<Agreement> findTopByFacultyIdOrderByIdDesc(Long facultyId);
}
