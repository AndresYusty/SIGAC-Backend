package com.universidad.sigac.service;

import com.universidad.sigac.dto.SesionDtos.SesionDto;
import com.universidad.sigac.dto.SesionDtos.SesionRequest;
import com.universidad.sigac.dto.SesionDtos.SesionResumenDto;
import com.universidad.sigac.entity.PuntoAgenda;
import com.universidad.sigac.entity.Sesion;
import com.universidad.sigac.entity.Solicitud;
import com.universidad.sigac.entity.TipoConsejo;
import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.enums.EstadoSesion;
import com.universidad.sigac.enums.EstadoSolicitud;
import com.universidad.sigac.enums.RolNombre;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.FacultadRepository;
import com.universidad.sigac.repository.SesionRepository;
import com.universidad.sigac.repository.SolicitudRepository;
import com.universidad.sigac.repository.TipoConsejoRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import com.universidad.sigac.util.FechaUtil;
import com.universidad.sigac.util.HtmlSanitizer;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Módulo 3 (Secretaría 1): sesiones, constructor del Orden del Día y cierre de agenda con citación (RF-18, RF-19). */
@Service
@RequiredArgsConstructor
public class AgendaService {

    private static final String PLANTILLA_CITACION = leerPlantillaCitacion();

    private final SesionRepository sesiones;
    private final SolicitudRepository solicitudes;
    private final TipoConsejoRepository tiposConsejo;
    private final FacultadRepository facultades;
    private final UsuarioRepository usuarios;
    private final SolicitudService solicitudService;
    private final PdfService pdf;
    private final StorageService storage;
    private final EmailService email;
    private final AuditService auditoria;

    @Transactional
    public SesionDto crear(SesionRequest req) {
        UsuarioAutenticado u = SecurityUtils.current();
        if (u.facultadId() == null) {
            throw ApiException.forbidden("Su usuario no está vinculado a una Facultad");
        }
        TipoConsejo tipo = tiposConsejo.findById(req.tipoConsejoId())
                .orElseThrow(() -> ApiException.notFound("Tipo de consejo no encontrado"));
        Sesion s = new Sesion();
        s.setTipoConsejo(tipo);
        s.setFacultad(facultades.getReferenceById(u.facultadId()));
        s.setFechaProgramada(req.fechaProgramada());
        s.setLugar(req.lugar().trim());
        sesiones.save(s);
        auditoria.registrar("EVENT_SESSION_CREATED", "SESION", s.getId(), tipo.getNombre());
        return SesionMapper.aDto(s);
    }

    @Transactional(readOnly = true)
    public Page<SesionResumenDto> listar(EstadoSesion estado, Pageable pageable) {
        UsuarioAutenticado u = SecurityUtils.current();
        Specification<Sesion> filtro = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!u.esGlobal()) {
                ps.add(cb.equal(root.get("facultad").get("id"), u.facultadId()));
            }
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return sesiones.findAll(filtro, pageable).map(SesionMapper::aResumen);
    }

    @Transactional(readOnly = true)
    public SesionDto obtener(Long id) {
        return SesionMapper.aDto(cargarConAcceso(id));
    }

    // ---------------------------------------------------------------- Orden del Día

    @Transactional
    public SesionDto agregarPunto(Long sesionId, Long solicitudId) {
        UsuarioAutenticado u = SecurityUtils.current();
        Sesion s = cargarConAgendaAbierta(sesionId);
        Solicitud sol = solicitudes.findById(solicitudId).orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));
        if (!sol.getFacultad().getId().equals(s.getFacultad().getId())) {
            throw ApiException.badRequest("La solicitud pertenece a otra Facultad");
        }
        if (sol.getEstado() != EstadoSolicitud.APROBADO) {
            throw ApiException.conflict("Solo pueden incluirse solicitudes APROBADAS (estado actual: " + sol.getEstado() + ")");
        }
        PuntoAgenda p = new PuntoAgenda();
        p.setSesion(s);
        p.setOrden(s.getPuntos().size() + 1);
        p.setTitulo(sol.getTitulo());
        p.setDescripcion(sol.getDescripcion());
        p.setSolicitud(sol);
        s.getPuntos().add(p);
        solicitudService.cambiarEstado(sol, EstadoSolicitud.EN_AGENDA, "Incluida en el Orden del Día de la sesión " + s.getId(), u.nombre());
        sesiones.saveAndFlush(s);
        auditoria.registrar("EVENT_AGENDA_POINT_ADDED", "SESION", s.getId(), sol.getCodigo());
        return SesionMapper.aDto(s);
    }

    @Transactional
    public SesionDto quitarPunto(Long sesionId, Long puntoId) {
        UsuarioAutenticado u = SecurityUtils.current();
        Sesion s = cargarConAgendaAbierta(sesionId);
        PuntoAgenda p = buscarPunto(s, puntoId);
        s.getPuntos().remove(p);
        if (p.getSolicitud() != null) {
            solicitudService.cambiarEstado(p.getSolicitud(), EstadoSolicitud.APROBADO, "Retirada del Orden del Día", u.nombre());
        }
        renumerar(s);
        sesiones.saveAndFlush(s);
        return SesionMapper.aDto(s);
    }

    /** RF-18 (drag & drop): recibe los ids de los puntos en el nuevo orden. */
    @Transactional
    public SesionDto reordenar(Long sesionId, List<Long> idsEnOrden) {
        Sesion s = cargarConAgendaAbierta(sesionId);
        Set<Long> actuales = s.getPuntos().stream().map(PuntoAgenda::getId).collect(Collectors.toSet());
        if (idsEnOrden.size() != actuales.size() || !actuales.equals(Set.copyOf(idsEnOrden))) {
            throw ApiException.badRequest("Debe enviar exactamente los puntos de la agenda, sin repetir");
        }
        int orden = 1;
        for (Long id : idsEnOrden) {
            buscarPunto(s, id).setOrden(orden++);
        }
        s.getPuntos().sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        return SesionMapper.aDto(s);
    }

    /** Congela la agenda (RN-09), genera la citación en PDF y la envía por correo a los integrantes. */
    @Transactional
    public SesionDto cerrarAgenda(Long sesionId) {
        Sesion s = cargarConAgendaAbierta(sesionId);
        if (s.getPuntos().isEmpty()) {
            throw ApiException.conflict("La agenda no tiene puntos: agregue al menos una solicitud aprobada");
        }
        byte[] citacion = pdf.generarPdfA(HtmlSanitizer.sanitizarDocumento(pdf.renderizar(PLANTILLA_CITACION, modeloCitacion(s))));
        String rutaCitacion = rutaCitacion(s.getId());
        storage.guardar(rutaCitacion, citacion);

        List<String> adjuntos = new ArrayList<>();
        adjuntos.add(rutaCitacion);
        s.getPuntos().stream().filter(p -> p.getSolicitud() != null)
                .forEach(p -> p.getSolicitud().getAnexos().forEach(a -> adjuntos.add(a.getRuta())));
        List<String> destinatarios = usuarios.findActivosPorFacultadYRoles(s.getFacultad().getId(),
                        List.of(RolNombre.ROLE_PRESIDENTE, RolNombre.ROLE_CONSEJERO, RolNombre.ROLE_SECRETARIA_1))
                .stream().map(Usuario::getEmail).distinct().toList();

        s.setAgendaCerrada(true);
        s.setEstado(EstadoSesion.CONVOCADA);
        auditoria.registrar("EVENT_AGENDA_CLOSED", "SESION", s.getId(), s.getPuntos().size() + " puntos");
        email.enviarCitacion(destinatarios, s.getTipoConsejo().getNombre(), s.getFacultad().getNombre(),
                FechaUtil.fechaLarga(s.getFechaProgramada().toLocalDate()), FechaUtil.hora(s.getFechaProgramada()),
                s.getLugar(), adjuntos);
        return SesionMapper.aDto(s);
    }

    @Transactional(readOnly = true)
    public byte[] descargarCitacion(Long sesionId) {
        Sesion s = cargarConAcceso(sesionId);
        if (!s.isAgendaCerrada()) {
            throw ApiException.conflict("La citación aún no se ha emitido: la agenda no está cerrada");
        }
        return storage.leer(rutaCitacion(s.getId()));
    }

    // ---------------------------------------------------------------- compartido con SesionEnVivoService

    /** Carga la sesión verificando que sea de la Facultad del usuario (RN-02). */
    public Sesion cargarConAcceso(Long id) {
        Sesion s = sesiones.findById(id).orElseThrow(() -> ApiException.notFound("Sesión no encontrada"));
        SecurityUtils.verificarFacultad(SecurityUtils.current(), s.getFacultad().getId());
        return s;
    }

    // ---------------------------------------------------------------- privados

    private Sesion cargarConAgendaAbierta(Long id) {
        Sesion s = cargarConAcceso(id);
        if (s.isAgendaCerrada()) {
            throw ApiException.conflict("La agenda ya fue cerrada y publicada; no admite modificaciones (RN-09)");
        }
        return s;
    }

    private PuntoAgenda buscarPunto(Sesion s, Long puntoId) {
        return s.getPuntos().stream().filter(p -> p.getId().equals(puntoId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("El punto no pertenece a esta sesión"));
    }

    private void renumerar(Sesion s) {
        int orden = 1;
        for (PuntoAgenda p : s.getPuntos()) {
            p.setOrden(orden++);
        }
    }

    private String rutaCitacion(Long sesionId) {
        return "citaciones/" + sesionId + "/citacion.pdf";
    }

    private Map<String, Object> modeloCitacion(Sesion s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("universidad", s.getFacultad().getSede().getUniversidad().getNombre());
        m.put("sede", s.getFacultad().getSede().getNombre());
        m.put("facultad", s.getFacultad().getNombre());
        m.put("tipoConsejo", s.getTipoConsejo().getNombre());
        m.put("fechaSesion", FechaUtil.fechaLarga(s.getFechaProgramada().toLocalDate()));
        m.put("horaSesion", FechaUtil.hora(s.getFechaProgramada()));
        m.put("lugar", s.getLugar());
        List<Map<String, Object>> orden = new ArrayList<>();
        for (PuntoAgenda p : s.getPuntos()) {
            Map<String, Object> punto = new LinkedHashMap<>();
            punto.put("numero", p.getOrden());
            punto.put("titulo", p.getTitulo());
            punto.put("descripcion", p.getDescripcion() == null || p.getDescripcion().isBlank() ? null : p.getDescripcion());
            orden.add(punto);
        }
        m.put("orden", orden);
        return m;
    }

    private static String leerPlantillaCitacion() {
        try (InputStream in = AgendaService.class.getResourceAsStream("/sigac/plantillas/citacion.html")) {
            if (in == null) {
                throw new IllegalStateException("Falta el recurso /sigac/plantillas/citacion.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible leer la plantilla de citación", e);
        }
    }
}
