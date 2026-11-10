package com.university.sigac.modules.agreements.repository;

import com.university.sigac.modules.agreements.entity.Agreement;
import com.university.sigac.modules.agreements.enums.DocumentKind;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AgreementRepository extends JpaRepository<Agreement, Long>, JpaSpecificationExecutor<Agreement> {

    @Query("select coalesce(max(a.number), 0) from Agreement a"
            + " where a.faculty.id = :facultyId and a.kind = :kind and a.year = :year")
    int lastNumber(Long facultyId, DocumentKind kind, int year);

    boolean existsByFacultyId(Long facultyId);

    boolean existsByFacultyIdAndKindAndYearAndNumberAndIdNot(Long facultyId, DocumentKind kind, int year, int number,
                                                            Long id);

    Optional<Agreement> findTopByFacultyIdOrderByIdDesc(Long facultyId);

    boolean existsByAgendaItemId(Long agendaItemId);
}
