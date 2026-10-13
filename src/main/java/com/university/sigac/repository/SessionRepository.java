package com.university.sigac.repository;

import com.university.sigac.entity.Session;
import com.university.sigac.enums.SessionStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SessionRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {

    long countByFacultyIdAndStatusIn(Long facultyId, Collection<SessionStatus> statuses);
}
