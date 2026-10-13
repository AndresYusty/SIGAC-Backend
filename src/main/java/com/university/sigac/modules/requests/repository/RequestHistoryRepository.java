package com.university.sigac.modules.requests.repository;

import com.university.sigac.modules.requests.entity.RequestHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    List<RequestHistory> findByRequestIdOrderByOccurredAtAscIdAsc(Long requestId);
}
