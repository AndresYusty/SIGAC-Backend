package com.university.sigac.core.sequence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SequenceCounterRepository extends JpaRepository<SequenceCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SequenceCounter c where c.counterKey = :key")
    Optional<SequenceCounter> findByKeyForUpdate(@Param("key") String key);
}
