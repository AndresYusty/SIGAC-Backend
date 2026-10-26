package com.university.sigac.modules.requests.repository;

import com.university.sigac.modules.requests.entity.AgendaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendaItemRepository extends JpaRepository<AgendaItem, Long> {

    @Modifying
    @Query("delete from AgendaItem i where i.request.id = :requestId")
    void deleteByRequestId(@Param("requestId") Long requestId);
}
