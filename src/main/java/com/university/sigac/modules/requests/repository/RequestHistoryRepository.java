package com.university.sigac.modules.requests.repository;

import com.university.sigac.modules.requests.entity.RequestHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    List<RequestHistory> findByRequestIdOrderByOccurredAtAscIdAsc(Long requestId);

    @Modifying
    @Query("delete from RequestHistory h where h.request.id = :requestId")
    void deleteByRequestId(@Param("requestId") Long requestId);
}
