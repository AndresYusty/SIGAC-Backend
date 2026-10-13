package com.university.sigac.repository;

import com.university.sigac.entity.RequestHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    List<RequestHistory> findByRequestIdOrderByOccurredAtAscIdAsc(Long requestId);
}
