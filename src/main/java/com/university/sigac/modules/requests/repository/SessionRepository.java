package com.university.sigac.modules.requests.repository;

import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.enums.SessionStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {

    boolean existsByFacultyId(Long facultyId);

    long countByFacultyIdAndStatusIn(Long facultyId, Collection<SessionStatus> statuses);

    Optional<Session> findBySupportsDownloadToken(String supportsDownloadToken);

    /** Ordinal of this session within the faculty and calendar year (for the citation heading). */
    @Query("""
            select count(s) from Session s
            where s.faculty.id = :facultyId
              and year(s.scheduledAt) = :year
              and s.id <= :sessionId
            """)
    long countNoticeOrdinal(@Param("facultyId") Long facultyId, @Param("year") int year, @Param("sessionId") Long sessionId);
}
