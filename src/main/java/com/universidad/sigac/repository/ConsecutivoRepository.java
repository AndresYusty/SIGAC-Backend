package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Consecutivo;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsecutivoRepository extends JpaRepository<Consecutivo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consecutivo c where c.clave = :clave")
    Optional<Consecutivo> findByClaveForUpdate(@Param("clave") String clave);
}
