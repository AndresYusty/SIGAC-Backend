package com.universidad.sigac.service;

import com.universidad.sigac.dto.ActaDtos.AuditLogDto;
import com.universidad.sigac.entity.AuditLog;
import com.universidad.sigac.repository.AuditLogRepository;
import com.universidad.sigac.security.ClientIpResolver;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bitácora de auditoría: quién, cuándo y desde qué IP (RF-40). Se guarda en una transacción propia para que el
 * registro persista aunque la operación principal falle. La tabla es append-only (RN-19).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ClientIpResolver ipResolver;

    /** Usa el usuario autenticado de la petición actual. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String evento, String entidad, Object entidadId, String detalle) {
        UsuarioAutenticado u = SecurityUtils.currentOrNull();
        guardar(evento, u == null ? null : u.id(), u == null ? null : u.email(), entidad, entidadId, detalle);
    }

    /** Para eventos sin sesión (login fallido, recuperación de clave). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarComo(String evento, Long usuarioId, String email, String detalle) {
        guardar(evento, usuarioId, email, "USUARIO", usuarioId, detalle);
    }

    /** Consulta de la bitácora (solo lectura). */
    @Transactional(readOnly = true)
    public Page<AuditLogDto> buscar(String evento, String usuario, Instant desde, Instant hasta, int page, int size) {
        PageRequest paginacion = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return repository.buscar(vacioANulo(evento), vacioANulo(usuario), desde, hasta, paginacion)
                .map(a -> new AuditLogDto(a.getId(), a.getEvento(), a.getUsuarioEmail(), a.getIp(), a.getEntidad(),
                        a.getEntidadId(), a.getDetalle(), a.getFecha()));
    }

    private String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void guardar(String evento, Long usuarioId, String email, String entidad, Object entidadId, String detalle) {
        try {
            AuditLog a = new AuditLog();
            a.setEvento(evento);
            a.setUsuarioId(usuarioId);
            a.setUsuarioEmail(email);
            a.setIp(ipResolver.current());
            a.setEntidad(entidad);
            a.setEntidadId(entidadId == null ? null : String.valueOf(entidadId));
            a.setDetalle(detalle != null && detalle.length() > 2000 ? detalle.substring(0, 2000) : detalle);
            a.setFecha(Instant.now());
            repository.save(a);
        } catch (RuntimeException e) {
            log.error("No fue posible registrar el evento de auditoría {}", evento, e);
        }
    }
}
