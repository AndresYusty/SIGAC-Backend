package com.university.sigac.modules.session.service;

import com.university.sigac.modules.session.repository.AttendanceRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only attendance queries offered to the other modules, so that AttendanceRepository stays internal. */
@Service
@RequiredArgsConstructor
public class AttendanceQueryService {

    private final AttendanceRepository attendances;

    /** Sessions in which the user was marked present: the only minutes a council member can see. */
    @Transactional(readOnly = true)
    public Set<Long> attendedSessionIds(Long userId) {
        return Set.copyOf(attendances.findAttendedSessionIds(userId));
    }

    @Transactional(readOnly = true)
    public List<String> presentEmails(Long sessionId) {
        return attendances.findPresentActiveEmails(sessionId);
    }
}
