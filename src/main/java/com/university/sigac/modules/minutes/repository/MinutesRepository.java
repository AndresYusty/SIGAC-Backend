package com.university.sigac.modules.minutes.repository;

import com.university.sigac.modules.minutes.entity.Minutes;
import com.university.sigac.modules.minutes.enums.MinutesStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MinutesRepository extends JpaRepository<Minutes, Long>, JpaSpecificationExecutor<Minutes> {

    boolean existsBySessionId(Long sessionId);

    Optional<Minutes> findBySessionId(Long sessionId);

    boolean existsByFacultyId(Long facultyId);

    List<Minutes> findByStatusIn(Collection<MinutesStatus> statuses);

    long countByStatus(MinutesStatus status);

    long countByFacultyIdAndStatusIn(Long facultyId, Collection<MinutesStatus> statuses);

    Page<Minutes> findByStatus(MinutesStatus status, Pageable pageable);

    Page<Minutes> findByFacultyId(Long facultyId, Pageable pageable);

    Page<Minutes> findByFacultyIdAndStatus(Long facultyId, MinutesStatus status, Pageable pageable);

    List<Minutes> findTop5ByFacultyIdAndStatusOrderBySignedAtDesc(Long facultyId, MinutesStatus status);

    List<Minutes> findTop5BySessionIdInAndStatusOrderBySignedAtDesc(Collection<Long> sessionIds, MinutesStatus status);
}
