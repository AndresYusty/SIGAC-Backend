package com.university.sigac.modules.requests.repository;

import com.university.sigac.modules.requests.entity.CouncilRequest;
import com.university.sigac.modules.requests.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CouncilRequestRepository extends JpaRepository<CouncilRequest, Long>, JpaSpecificationExecutor<CouncilRequest> {

    long countByFacultyIdAndStatus(Long facultyId, RequestStatus status);

    long countByFiledByIdAndStatus(Long filedById, RequestStatus status);
}
