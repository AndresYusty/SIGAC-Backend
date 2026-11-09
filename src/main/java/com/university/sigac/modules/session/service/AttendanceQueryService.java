package com.university.sigac.modules.session.service;

import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.repository.SessionRepository;
import com.university.sigac.modules.session.repository.AttendanceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only attendance queries offered to the other modules, so that AttendanceRepository stays internal. */
@Service
@RequiredArgsConstructor
public class AttendanceQueryService {

    private final AttendanceRepository attendances;
    private final SessionRepository sessions;

    /** Sessions in which the user was marked present. */
    @Transactional(readOnly = true)
    public Set<Long> attendedSessionIds(Long userId) {
        return Set.copyOf(attendances.findAttendedSessionIds(userId));
    }

    /** Calendar days of the sessions the person attended. An agreement of another day is not theirs. */
    @Transactional(readOnly = true)
    public Set<LocalDate> attendedSessionDates(Set<Long> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Set.of();
        }
        return sessions.findAllById(sessionIds).stream()
                .flatMap(AttendanceQueryService::datesOf)
                .collect(Collectors.toSet());
    }

    private static Stream<LocalDate> datesOf(Session session) {
        Stream.Builder<LocalDate> dates = Stream.builder();
        if (session.getScheduledAt() != null) {
            dates.add(session.getScheduledAt().toLocalDate());
        }
        if (session.getActualStart() != null) {
            dates.add(session.getActualStart().toLocalDate());
        }
        return dates.build();
    }

    @Transactional(readOnly = true)
    public List<String> presentEmails(Long sessionId) {
        return attendances.findPresentActiveEmails(sessionId);
    }
}
