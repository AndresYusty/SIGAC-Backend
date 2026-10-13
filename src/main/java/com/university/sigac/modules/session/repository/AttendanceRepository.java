package com.university.sigac.modules.session.repository;

import com.university.sigac.modules.session.entity.Attendance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySessionId(Long sessionId);

    Optional<Attendance> findBySessionIdAndUserId(Long sessionId, Long userId);
}
