package com.university.sigac.modules.repository.service;

import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.modules.minutes.entity.Minutes;
import com.university.sigac.modules.minutes.enums.MinutesStatus;
import com.university.sigac.modules.minutes.service.MinutesQueryService;
import com.university.sigac.modules.repository.dto.RepositoryDtos.DashboardDto;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.requests.service.AgendaService;
import com.university.sigac.modules.requests.service.CouncilRequestService;
import com.university.sigac.modules.users.service.UserLookupService;
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

    private final CouncilRequestService requests;
    private final AgendaService sessions;
    private final MinutesQueryService minutes;
    private final UserLookupService users;

    @Transactional(readOnly = true)
    public DashboardDto get() {
        AuthenticatedUser u = SecurityUtils.current();
        Long fac = u.facultyId();
        Map<String, Object> ind = new LinkedHashMap<>();

        if (u.hasRole("ROLE_SECRETARY_2")) {
            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (RequestStatus s : RequestStatus.values()) {
                byStatus.put(s.name(), requests.countFiledBy(u.id(), s));
            }
            ind.put("myRequestsByStatus", byStatus);
        }
        if (u.hasRole("ROLE_SECRETARY_1") && fac != null) {
            ind.put("requestsPendingReview", requests.countByFaculty(fac, RequestStatus.FILED));
            ind.put("approvedRequestsWithoutAgenda", requests.countByFaculty(fac, RequestStatus.APPROVED));
            ind.put("upcomingSessions", sessions.countByFaculty(fac, List.of(SessionStatus.SCHEDULED, SessionStatus.CONVENED)));
            ind.put("minutesInProgress", minutes.countByFaculty(fac, List.of(MinutesStatus.DRAFT, MinutesStatus.EDITING)));
        }
        if (u.hasAnyRole("ROLE_SECRETARY_1", "ROLE_PRESIDENT", "ROLE_COUNCIL_MEMBER") && fac != null) {
            ind.put("minutesPendingSignature", minutes.countByFaculty(fac, List.of(MinutesStatus.PENDING_SIGNATURE)));
            ind.put("finishedSessions", sessions.countByFaculty(fac, List.of(SessionStatus.FINISHED)));
            List<Map<String, Object>> latest = new ArrayList<>();
            for (Minutes a : minutes.latestSigned(fac, MinutesStatus.PUBLISHED)) {
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
