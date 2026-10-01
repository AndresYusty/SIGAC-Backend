package com.universidad.sigac.repository;

import com.universidad.sigac.entity.AuditLog;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Repositorio deliberadamente limitado: solo inserción y consulta (sin update ni delete). */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog log);

    @Query("select a from AuditLog a where (:evento is null or a.evento = :evento) "
            + "and (:email is null or lower(a.usuarioEmail) like lower(concat('%', :email, '%'))) "
            + "and (:desde is null or a.fecha >= :desde) and (:hasta is null or a.fecha <= :hasta) "
            + "order by a.fecha desc")
    Page<AuditLog> buscar(@Param("evento") String evento, @Param("email") String email,
                          @Param("desde") Instant desde, @Param("hasta") Instant hasta, Pageable pageable);
}
