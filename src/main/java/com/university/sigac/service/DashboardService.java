package com.university.sigac.service;

import com.university.sigac.dto.MinutesDtos.DashboardDto;
import com.university.sigac.entity.Minutes;
import com.university.sigac.enums.MinutesStatus;
import com.university.sigac.enums.RequestStatus;
import com.university.sigac.enums.SessionStatus;
import com.university.sigac.repository.CouncilRequestRepository;
import com.university.sigac.repository.MinutesRepository;
import com.university.sigac.repository.SessionRepository;
import com.university.sigac.repository.UserRepository;
import com.university.sigac.security.AuthenticatedUser;
import com.university.sigac.security.SecurityUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Simple indicators depending on the user's role. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CouncilRequestRepository requests;
    private final SessionRepository sessions;
    private final MinutesRepository minutes;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public DashboardDto get() {
        AuthenticatedUser u = SecurityUtils.current();
        Long fac = u.facultyId();
        Map<String, Object> ind = new LinkedHashMap<>();

        if (u.hasRole("ROLE_SECRETARY_2")) {
            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (RequestStatus s : RequestStatus.values()) {
                byStatus.put(s.name(), requests.countByFiledByIdAndStatus(u.id(), s));
            }
            ind.put("myRequestsByStatus", byStatus);
        }
        if (u.hasRole("ROLE_SECRETARY_1") && fac != null) {
            ind.put("requestsPendingReview", requests.countByFacultyIdAndStatus(fac, RequestStatus.FILED));
            ind.put("approvedRequestsWithoutAgenda", requests.countByFacultyIdAndStatus(fac, RequestStatus.APPROVED));
            ind.put("upcomingSessions", sessions.countByFacultyIdAndStatusIn(fac, List.of(SessionStatus.SCHEDULED, SessionStatus.CONVENED)));
            ind.put("minutesInProgress", minutes.countByFacultyIdAndStatusIn(fac, List.of(MinutesStatus.DRAFT, MinutesStatus.EDITING)));
        }
        if (u.hasAnyRole("ROLE_SECRETARY_1", "ROLE_PRESIDENT", "ROLE_COUNCIL_MEMBER") && fac != null) {
            ind.put("minutesPendingSignature", minutes.countByFacultyIdAndStatusIn(fac, List.of(MinutesStatus.PENDING_SIGNATURE)));
            ind.put("finishedSessions", sessions.countByFacultyIdAndStatusIn(fac, List.of(SessionStatus.FINISHED)));
            List<Map<String, Object>> latest = new ArrayList<>();
            for (Minutes a : minutes.findTop5ByFacultyIdAndStatusOrderBySignedAtDesc(fac, MinutesStatus.PUBLISHED)) {
                latest.add(Map.of("id", a.getId(), "serialNumber", a.getSerialNumber(), "sessionDate", a.getSessionDate()));
            }
            ind.put("latestPublishedMinutes", latest);
        }
        if (u.hasRole("ROLE_ADMIN")) {
            ind.put("activeUsers", users.countByActive(true));
            ind.put("inactiveUsers", users.countByActive(false));
            ind.put("publishedMinutesTotal", minutes.countByStatus(MinutesStatus.PUBLISHED));
        }
        return new DashboardDto(u.name(), ind);
    }
}
