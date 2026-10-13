package com.university.sigac.modules.minutes.service;

import com.university.sigac.modules.minutes.entity.Minutes;
import com.university.sigac.modules.minutes.enums.MinutesStatus;
import com.university.sigac.modules.minutes.repository.MinutesRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** Read-only minutes queries offered to the other modules, so that MinutesRepository stays internal to this module. */
@Service
@RequiredArgsConstructor
public class MinutesQueryService {

    private final MinutesRepository minutes;

    public Optional<Minutes> findById(Long id) {
        return minutes.findById(id);
    }

    public Page<Minutes> findAll(Specification<Minutes> filter, Pageable pageable) {
        return minutes.findAll(filter, pageable);
    }

    public long countByStatus(MinutesStatus status) {
        return minutes.countByStatus(status);
    }

    public long countByFaculty(Long facultyId, Collection<MinutesStatus> statuses) {
        return minutes.countByFacultyIdAndStatusIn(facultyId, statuses);
    }

    /** The 5 most recently signed minutes of a Faculty in the given status. */
    public List<Minutes> latestSigned(Long facultyId, MinutesStatus status) {
        return minutes.findTop5ByFacultyIdAndStatusOrderBySignedAtDesc(facultyId, status);
    }
}
