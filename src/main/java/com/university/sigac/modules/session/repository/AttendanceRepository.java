package com.university.sigac.modules.session.repository;

import com.university.sigac.modules.session.entity.Attendance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySessionId(Long sessionId);

    @Modifying
    @Query("delete from Attendance a where a.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);

    Optional<Attendance> findBySessionIdAndUserId(Long sessionId, Long userId);

    @Query("select a.session.id from Attendance a where a.user.id = :userId and a.present = true")
    List<Long> findAttendedSessionIds(@Param("userId") Long userId);

    @Query("select a.user.email from Attendance a"
            + " where a.session.id = :sessionId and a.present = true and a.user.active = true")
    List<String> findPresentActiveEmails(@Param("sessionId") Long sessionId);
}
