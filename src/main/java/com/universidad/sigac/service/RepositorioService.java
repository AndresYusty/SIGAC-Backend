package com.universidad.sigac.service;

import com.universidad.sigac.dto.ActaDtos.ArchivoActa;
import com.universidad.sigac.dto.ActaDtos.RepositorioActaDto;
import com.universidad.sigac.dto.ActaDtos.VerificacionDto;
import com.universidad.sigac.entity.Acta;
import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.enums.EstadoActa;
import com.universidad.sigac.enums.RolNombre;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.ActaRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import com.universidad.sigac.util.HashUtil;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Módulo 7: búsqueda de actas publicadas y entrega de archivos con verificación SHA-256 (RN-18). */
@Service
@RequiredArgsConstructor
public class RepositorioService {

    private final ActaRepository actas;
    private final UsuarioRepository usuarios;
    private final StorageService storage;
    private final AuditService auditoria;
    private final EmailService email;

    /** RF-37: solo actas PUBLICADAS, de la Facultad del usuario salvo perfiles globales (RN-02). */
    @Transactional(readOnly = true)
    public Page<RepositorioActaDto> buscar(String consecutivo, Long tipoConsejoId, LocalDate desde, LocalDate hasta,
                                           Long sedeId, Long facultadId, String texto, Pageable pageable) {
        UsuarioAutenticado u = SecurityUtils.current();
        Specification<Acta> filtro = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("estado"), EstadoActa.PUBLICADA));
            if (!u.esGlobal()) {
                ps.add(cb.equal(root.get("facultad").get("id"), u.facultadId()));
            } else if (facultadId != null) {
                ps.add(cb.equal(root.get("facultad").get("id"), facultadId));
            }
            if (consecutivo != null && !consecutivo.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("consecutivo")), "%" + consecutivo.trim().toLowerCase() + "%"));
            }
            if (tipoConsejoId != null) {
                ps.add(cb.equal(root.get("tipoConsejo").get("id"), tipoConsejoId));
            }
            if (sedeId != null) {
                ps.add(cb.equal(root.get("facultad").get("sede").get("id"), sedeId));
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("fechaSesion"), desde));
            }
            if (hasta != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("fechaSesion"), hasta));
            }
            if (texto != null && !texto.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("contenidoHtml")), "%" + texto.trim().toLowerCase() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return actas.findAll(filtro, pageable).map(this::aDto);
    }

    /**
     * RN-18 / CU-07: recalcula el SHA-256 del archivo en disco y lo compara con el registrado al firmar.
     * Si no coincide, bloquea la entrega, deja el evento en la bitácora y avisa al perfil técnico.
     */
    @Transactional(readOnly = true)
    public ArchivoActa leerVerificado(Long actaId, String evento) {
        Acta a = cargarPublicada(actaId);
        if (!storage.existe(a.getRutaArchivo())) {
            reportarCompromiso(a, "Archivo no encontrado en el almacenamiento");
            throw ApiException.conflict("Documento comprometido: el archivo no se encuentra en el almacenamiento");
        }
        byte[] datos = storage.leer(a.getRutaArchivo());
        String calculado = HashUtil.sha256Hex(datos);
        if (!HashUtil.iguales(calculado, a.getHashArchivo())) {
            reportarCompromiso(a, "Hash recalculado " + calculado + " distinto del registrado " + a.getHashArchivo());
            throw ApiException.conflict("Documento comprometido: la huella SHA-256 no coincide con la registrada al firmar. "
                    + "Se bloqueó la descarga y se notificó al perfil técnico");
        }
        auditoria.registrar(evento, "ACTA", a.getId(), a.getConsecutivo() + " sha256=" + calculado);
        return new ArchivoActa(a.getConsecutivo() + ".pdf", datos, calculado);
    }

    @Transactional(readOnly = true)
    public VerificacionDto verificar(Long actaId) {
        Acta a = cargarPublicada(actaId);
        String calculado = storage.existe(a.getRutaArchivo()) ? HashUtil.sha256Hex(storage.leer(a.getRutaArchivo())) : null;
        boolean integro = HashUtil.iguales(calculado, a.getHashArchivo());
        if (!integro) {
            reportarCompromiso(a, "Verificación manual: hash calculado " + calculado);
        }
        return new VerificacionDto(a.getConsecutivo(), a.getHashArchivo(), calculado, integro);
    }

    private void reportarCompromiso(Acta a, String detalle) {
        auditoria.registrar("EVENT_INTEGRITY_VIOLATION", "ACTA", a.getId(), a.getConsecutivo() + ": " + detalle);
        List<String> tecnicos = usuarios.findActivosPorRoles(List.of(RolNombre.ROLE_DEV)).stream().map(Usuario::getEmail).toList();
        email.enviarAlertaIntegridad(tecnicos, a.getConsecutivo());
    }

    private Acta cargarPublicada(Long id) {
        Acta a = actas.findById(id).orElseThrow(() -> ApiException.notFound("Acta no encontrada"));
        SecurityUtils.verificarFacultad(SecurityUtils.current(), a.getFacultad().getId());
        if (a.getEstado() != EstadoActa.PUBLICADA) {
            throw ApiException.conflict("Solo las actas PUBLICADAS están disponibles en el repositorio");
        }
        return a;
    }

    private RepositorioActaDto aDto(Acta a) {
        var f = a.getFacultad();
        return new RepositorioActaDto(a.getId(), a.getConsecutivo(), a.getTipoConsejo().getNombre(),
                f.getSede().getUniversidad().getNombre(), f.getSede().getNombre(), f.getNombre(), a.getFechaSesion(),
                a.getFirmadoPor(), a.getFirmadoEn(), a.getHashArchivo());
    }
}
