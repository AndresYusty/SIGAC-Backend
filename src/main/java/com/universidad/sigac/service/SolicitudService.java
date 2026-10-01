package com.universidad.sigac.service;

import com.universidad.sigac.config.AppProperties;
import com.universidad.sigac.dto.SolicitudDtos.AnexoDto;
import com.universidad.sigac.dto.SolicitudDtos.Archivo;
import com.universidad.sigac.dto.SolicitudDtos.HistorialDto;
import com.universidad.sigac.dto.SolicitudDtos.SolicitudDto;
import com.universidad.sigac.dto.SolicitudDtos.SolicitudRequest;
import com.universidad.sigac.entity.Anexo;
import com.universidad.sigac.entity.HistorialSolicitud;
import com.universidad.sigac.entity.Programa;
import com.universidad.sigac.entity.Solicitud;
import com.universidad.sigac.enums.EstadoSolicitud;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.FacultadRepository;
import com.universidad.sigac.repository.HistorialSolicitudRepository;
import com.universidad.sigac.repository.ProgramaRepository;
import com.universidad.sigac.repository.SolicitudRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import com.universidad.sigac.util.HashUtil;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Módulo 3: radicación (Secretaría 2) y evaluación (Secretaría 1) de solicitudes. */
@Service
@RequiredArgsConstructor
public class SolicitudService {

    private static final Set<EstadoSolicitud> MODIFICABLES = Set.of(EstadoSolicitud.RADICADO, EstadoSolicitud.DEVUELTO);

    private final SolicitudRepository solicitudes;
    private final HistorialSolicitudRepository historial;
    private final UsuarioRepository usuarios;
    private final FacultadRepository facultades;
    private final ProgramaRepository programas;
    private final ParametroService parametros;
    private final StorageService storage;
    private final ConsecutivoService consecutivos;
    private final AuditService auditoria;
    private final EmailService email;
    private final AppProperties props;

    // ================================================================ Secretaría 2

    @Transactional
    public SolicitudDto radicar(SolicitudRequest req, List<MultipartFile> archivos) {
        UsuarioAutenticado u = SecurityUtils.current();
        if (u.facultadId() == null) {
            throw ApiException.forbidden("Su usuario no está vinculado a una Facultad");
        }
        validarPdfs(archivos);

        Solicitud s = new Solicitud();
        s.setTitulo(req.titulo().trim());
        s.setDescripcion(req.descripcion().trim());
        s.setSolicitante(req.solicitante().trim());
        s.setTipoSolicitud(req.tipoSolicitud());
        s.setFacultad(facultades.getReferenceById(u.facultadId()));
        s.setPrograma(resolverPrograma(req.programaId(), u));
        s.setRadicador(usuarios.getReferenceById(u.id()));
        int anio = Year.now(ZoneId.of(props.getZone())).getValue();
        s.setCodigo("SOL-" + anio + "-" + String.format("%06d", consecutivos.siguiente("SOL-" + anio)));
        solicitudes.save(s);

        guardarAnexos(s, archivos);
        registrarHistorial(s, null, EstadoSolicitud.RADICADO, "Solicitud radicada", u.nombre());
        auditoria.registrar("EVENT_REQUEST_CREATED", "SOLICITUD", s.getId(), s.getCodigo());
        return aDto(s);
    }

    /** Corrección de una solicitud RADICADA o DEVUELTA (RN-09: las demás ya no se pueden modificar). */
    @Transactional
    public SolicitudDto actualizar(Long id, SolicitudRequest req, List<MultipartFile> nuevosAnexos) {
        UsuarioAutenticado u = SecurityUtils.current();
        Solicitud s = cargar(id);
        if (!s.getRadicador().getId().equals(u.id())) {
            throw ApiException.forbidden("Solo quien radicó la solicitud puede modificarla");
        }
        if (!MODIFICABLES.contains(s.getEstado())) {
            throw ApiException.conflict("La solicitud no puede modificarse en estado " + s.getEstado());
        }
        validarPdfs(nuevosAnexos);
        s.setTitulo(req.titulo().trim());
        s.setDescripcion(req.descripcion().trim());
        s.setSolicitante(req.solicitante().trim());
        s.setTipoSolicitud(req.tipoSolicitud());
        s.setPrograma(resolverPrograma(req.programaId(), u));
        guardarAnexos(s, nuevosAnexos);
        if (s.getEstado() == EstadoSolicitud.DEVUELTO) {
            cambiarEstado(s, EstadoSolicitud.RADICADO, "Reenviada tras correcciones", u.nombre());
        }
        auditoria.registrar("EVENT_REQUEST_UPDATED", "SOLICITUD", s.getId(), s.getCodigo());
        return aDto(s);
    }

    // ================================================================ Consulta

    @Transactional(readOnly = true)
    public Page<SolicitudDto> buscar(EstadoSolicitud estado, String codigo, LocalDate desde, LocalDate hasta, Pageable pageable) {
        UsuarioAutenticado u = SecurityUtils.current();
        ZoneId zona = ZoneId.of(props.getZone());
        Specification<Solicitud> filtro = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!u.esGlobal()) {
                // Secretaría 2 ve solo lo que radicó; los demás roles, lo de su Facultad (RN-02)
                if (u.tieneRol("ROLE_SECRETARIA_2")) {
                    ps.add(cb.equal(root.get("radicador").get("id"), u.id()));
                } else {
                    ps.add(cb.equal(root.get("facultad").get("id"), u.facultadId()));
                }
            }
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (codigo != null && !codigo.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("codigo")), "%" + codigo.trim().toLowerCase() + "%"));
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("fechaRadicacion"), desde.atStartOfDay(zona).toInstant()));
            }
            if (hasta != null) {
                ps.add(cb.lessThan(root.get("fechaRadicacion"), hasta.plusDays(1).atStartOfDay(zona).toInstant()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return solicitudes.findAll(filtro, pageable).map(this::aDto);
    }

    @Transactional(readOnly = true)
    public SolicitudDto obtener(Long id) {
        return aDto(cargarConAcceso(id));
    }

    @Transactional(readOnly = true)
    public List<HistorialDto> historial(Long id) {
        cargarConAcceso(id);
        return historial.findBySolicitudIdOrderByFechaAscIdAsc(id).stream()
                .map(h -> new HistorialDto(h.getEstadoAnterior(), h.getEstadoNuevo(), h.getMotivo(), h.getUsuarioNombre(), h.getFecha()))
                .toList();
    }

    /** Descarga un anexo verificando que su huella SHA-256 no haya cambiado. */
    @Transactional(readOnly = true)
    public Archivo descargarAnexo(Long solicitudId, Long anexoId) {
        Solicitud s = cargarConAcceso(solicitudId);
        Anexo a = s.getAnexos().stream().filter(x -> x.getId().equals(anexoId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Anexo no encontrado"));
        byte[] datos = storage.leer(a.getRuta());
        if (!HashUtil.iguales(HashUtil.sha256Hex(datos), a.getHashSha256())) {
            auditoria.registrar("EVENT_INTEGRITY_VIOLATION", "ANEXO", a.getId(), a.getNombreOriginal());
            throw ApiException.conflict("El anexo fue alterado: su huella SHA-256 no coincide con la registrada");
        }
        return new Archivo(a.getNombreOriginal(), datos);
    }

    // ================================================================ Secretaría 1: evaluación

    @Transactional
    public SolicitudDto aprobar(Long id) {
        UsuarioAutenticado u = SecurityUtils.current();
        Solicitud s = cargarParaEvaluar(id, u);
        cambiarEstado(s, EstadoSolicitud.APROBADO, "Aprobada por la Secretaría 1", u.nombre());
        auditoria.registrar("EVENT_REQUEST_APPROVED", "SOLICITUD", s.getId(), s.getCodigo());
        return aDto(s);
    }

    @Transactional
    public SolicitudDto rechazar(Long id, String motivo) {
        UsuarioAutenticado u = SecurityUtils.current();
        Solicitud s = cargarParaEvaluar(id, u);
        cambiarEstado(s, EstadoSolicitud.RECHAZADO, motivo.trim(), u.nombre());
        auditoria.registrar("EVENT_REQUEST_REJECTED", "SOLICITUD", s.getId(), s.getCodigo());
        email.enviarSolicitudDevuelta(s.getRadicador().getEmail(), s.getCodigo(), s.getTitulo(), motivo.trim(), true);
        return aDto(s);
    }

    /** RN-10: la devolución exige justificación textual, que queda en el historial. */
    @Transactional
    public SolicitudDto devolver(Long id, String motivo) {
        UsuarioAutenticado u = SecurityUtils.current();
        Solicitud s = cargarParaEvaluar(id, u);
        cambiarEstado(s, EstadoSolicitud.DEVUELTO, motivo.trim(), u.nombre());
        auditoria.registrar("EVENT_REQUEST_RETURNED", "SOLICITUD", s.getId(), s.getCodigo());
        email.enviarSolicitudDevuelta(s.getRadicador().getEmail(), s.getCodigo(), s.getTitulo(), motivo.trim(), false);
        return aDto(s);
    }

    // ================================================================ Uso desde otros servicios

    /** Cambia el estado y deja huella en el historial. Debe llamarse dentro de una transacción. */
    public void cambiarEstado(Solicitud s, EstadoSolicitud nuevo, String motivo, String usuarioNombre) {
        EstadoSolicitud anterior = s.getEstado();
        s.setEstado(nuevo);
        registrarHistorial(s, anterior, nuevo, motivo, usuarioNombre);
    }

    // ================================================================ privados

    private void registrarHistorial(Solicitud s, EstadoSolicitud anterior, EstadoSolicitud nuevo, String motivo, String usuario) {
        HistorialSolicitud h = new HistorialSolicitud();
        h.setSolicitud(s);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setMotivo(motivo);
        h.setUsuarioNombre(usuario);
        historial.save(h);
    }

    /** RN-08: solo PDF (extensión + firma %PDF-) y tamaño máximo parametrizable (10 MB por defecto). */
    private void validarPdfs(List<MultipartFile> archivos) {
        if (archivos == null) {
            return;
        }
        int maxMb = parametros.getInt(ParametroService.ARCHIVOS_MAX_MB, 10);
        for (MultipartFile f : archivos) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            String nombre = f.getOriginalFilename();
            if (nombre == null || !nombre.toLowerCase().endsWith(".pdf")) {
                throw ApiException.badRequest("Solo se permiten documentos en formato PDF");
            }
            if (f.getSize() > (long) maxMb * 1024 * 1024) {
                throw ApiException.payloadTooLarge("El documento '" + nombre + "' excede el límite máximo de " + maxMb + " MB");
            }
            try (InputStream in = f.getInputStream()) {
                if (!"%PDF-".equals(new String(in.readNBytes(5), StandardCharsets.ISO_8859_1))) {
                    throw ApiException.badRequest("El archivo '" + nombre + "' no es un PDF válido");
                }
            } catch (IOException e) {
                throw ApiException.badRequest("No fue posible leer el archivo '" + nombre + "'");
            }
        }
    }

    /** Guarda cada PDF en disco y registra su ruta y hash SHA-256. Si algo falla, borra lo ya escrito. */
    private void guardarAnexos(Solicitud s, List<MultipartFile> archivos) {
        if (archivos == null) {
            return;
        }
        List<String> escritos = new ArrayList<>();
        try {
            int anio = Year.now(ZoneId.of(props.getZone())).getValue();
            for (MultipartFile f : archivos) {
                if (f == null || f.isEmpty()) {
                    continue;
                }
                byte[] datos = f.getBytes();
                String ruta = "solicitudes/" + anio + "/" + s.getCodigo() + "/" + UUID.randomUUID() + "-"
                        + StorageService.slug(f.getOriginalFilename());
                storage.guardar(ruta, datos);
                escritos.add(ruta);
                Anexo a = new Anexo();
                a.setSolicitud(s);
                a.setNombreOriginal(f.getOriginalFilename());
                a.setRuta(ruta);
                a.setHashSha256(HashUtil.sha256Hex(datos));
                a.setTamanoBytes(datos.length);
                s.getAnexos().add(a);
            }
            solicitudes.saveAndFlush(s);
        } catch (IOException | RuntimeException e) {
            escritos.forEach(storage::eliminarSilencioso);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw ApiException.badRequest("No fue posible procesar los archivos adjuntos");
        }
    }

    private Programa resolverPrograma(Long programaId, UsuarioAutenticado u) {
        Long id = programaId != null ? programaId : u.programaId();
        if (id == null) {
            return null;
        }
        Programa p = programas.findById(id).orElseThrow(() -> ApiException.notFound("Programa no encontrado"));
        if (!p.getFacultad().getId().equals(u.facultadId())) {
            throw ApiException.badRequest("El Programa no pertenece a su Facultad");
        }
        return p;
    }

    private Solicitud cargar(Long id) {
        return solicitudes.findById(id).orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));
    }

    private Solicitud cargarConAcceso(Long id) {
        UsuarioAutenticado u = SecurityUtils.current();
        Solicitud s = cargar(id);
        SecurityUtils.verificarFacultad(u, s.getFacultad().getId());
        if (u.tieneRol("ROLE_SECRETARIA_2") && !u.esGlobal() && !s.getRadicador().getId().equals(u.id())) {
            throw ApiException.forbidden("Solo puede consultar las solicitudes que usted radicó");
        }
        return s;
    }

    private Solicitud cargarParaEvaluar(Long id, UsuarioAutenticado u) {
        Solicitud s = cargar(id);
        SecurityUtils.verificarFacultad(u, s.getFacultad().getId());
        if (s.getEstado() != EstadoSolicitud.RADICADO) {
            throw ApiException.conflict("Solo pueden evaluarse solicitudes RADICADAS (estado actual: " + s.getEstado() + ")");
        }
        return s;
    }

    private SolicitudDto aDto(Solicitud s) {
        return new SolicitudDto(s.getId(), s.getCodigo(), s.getTitulo(), s.getDescripcion(), s.getSolicitante(),
                s.getTipoSolicitud(), s.getEstado(), s.getFacultad().getId(), s.getFacultad().getNombre(),
                s.getPrograma() == null ? null : s.getPrograma().getId(),
                s.getPrograma() == null ? null : s.getPrograma().getNombre(),
                s.getRadicador().getNombre(), s.getFechaRadicacion(),
                s.getAnexos().stream().map(a -> new AnexoDto(a.getId(), a.getNombreOriginal(), a.getTamanoBytes(), a.getHashSha256())).toList());
    }
}
