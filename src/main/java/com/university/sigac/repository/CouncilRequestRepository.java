package com.university.sigac.repository;

import com.university.sigac.entity.CouncilRequest;
import com.university.sigac.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CouncilRequestRepository extends JpaRepository<CouncilRequest, Long>, JpaSpecificationExecutor<CouncilRequest> {

    long countByFacultyIdAndStatus(Long facultyId, RequestStatus status);

    long countByFiledByIdAndStatus(Long filedById, RequestStatus status);
}
