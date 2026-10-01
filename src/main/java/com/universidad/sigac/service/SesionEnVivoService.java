package com.universidad.sigac.service;

import com.universidad.sigac.dto.SesionDtos.AsistenciaDto;
import com.universidad.sigac.dto.SesionDtos.AsistenciaItem;
import com.universidad.sigac.dto.SesionDtos.AsistenciaRequest;
import com.universidad.sigac.dto.SesionDtos.AsistenteDto;
import com.universidad.sigac.dto.SesionDtos.DecisionRequest;
import com.universidad.sigac.dto.SesionDtos.NotaRequest;
import com.universidad.sigac.dto.SesionDtos.PuntoDto;
import com.universidad.sigac.dto.SesionDtos.PuntoVarioRequest;
import com.universidad.sigac.dto.SesionDtos.QuorumDto;
import com.universidad.sigac.dto.SesionDtos.SesionDto;
import com.universidad.sigac.entity.Asistencia;
import com.universidad.sigac.entity.PuntoAgenda;
import com.universidad.sigac.entity.Sesion;
import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.enums.EstadoSesion;
import com.universidad.sigac.enums.EstadoSolicitud;
import com.universidad.sigac.enums.RolNombre;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.AsistenciaRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import com.universidad.sigac.util.HtmlSanitizer;
import com.universidad.sigac.util.QuorumCalculator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.universidad.sigac.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Módulo 4: asistencia y quórum, control de la sesión, decisiones, notas de debate y puntos varios. */
@Service
@RequiredArgsConstructor
public class SesionEnVivoService {

    private final AgendaService agenda;
    private final AsistenciaRepository asistencias;
    private final UsuarioRepository usuarios;
    private final ParametroService parametros;
    private final SolicitudService solicitudService;
    private final AuditService auditoria;
    private final AppProperties props;

    // ---------------------------------------------------------------- Asistencia y quórum (RF-21)

    @Transactional(readOnly = true)
    public AsistenciaDto asistencia(Long sesionId) {
        return armarAsistencia(agenda.cargarConAcceso(sesionId));
    }

    @Transactional
    public AsistenciaDto registrarAsistencia(Long sesionId, AsistenciaRequest req) {
        Sesion s = agenda.cargarConAcceso(sesionId);
        if (!Set.of(EstadoSesion.CONVOCADA, EstadoSesion.INICIADA, EstadoSesion.SUSPENDIDA).contains(s.getEstado())) {
            throw ApiException.conflict("No se puede registrar asistencia con la sesión en estado " + s.getEstado());
        }
        Set<Long> integrantes = integrantes(s).stream().map(Usuario::getId).collect(Collectors.toSet());
        for (AsistenciaItem item : req.items()) {
            if (!integrantes.contains(item.usuarioId())) {
                throw ApiException.badRequest("El usuario " + item.usuarioId() + " no es integrante con voto de este Consejo");
            }
            Asistencia a = asistencias.findBySesionIdAndUsuarioId(s.getId(), item.usuarioId()).orElseGet(() -> {
                Asistencia nueva = new Asistencia();
                nueva.setSesion(s);
                nueva.setUsuario(usuarios.getReferenceById(item.usuarioId()));
                return nueva;
            });
            a.setPresente(item.presente());
            asistencias.save(a);
        }
        asistencias.flush();
        return armarAsistencia(s);
    }

    // ---------------------------------------------------------------- Control de la sesión (RF-22)

    @Transactional
    public SesionDto iniciar(Long sesionId) {
        Sesion s = agenda.cargarConAcceso(sesionId);
        exigirTransicion(s, EstadoSesion.INICIADA);
        exigirQuorum(s);
        s.setEstado(EstadoSesion.INICIADA);
        s.setInicioReal(LocalDateTime.now(ZoneId.of(props.getZone())));
        auditoria.registrar("EVENT_SESSION_STARTED", "SESION", s.getId(), null);
        return SesionMapper.aDto(s);
    }

    @Transactional
    public SesionDto suspender(Long sesionId) {
        Sesion s = agenda.cargarConAcceso(sesionId);
        exigirTransicion(s, EstadoSesion.SUSPENDIDA);
        s.setEstado(EstadoSesion.SUSPENDIDA);
        auditoria.registrar("EVENT_SESSION_SUSPENDED", "SESION", s.getId(), null);
        return SesionMapper.aDto(s);
    }

    @Transactional
    public SesionDto reanudar(Long sesionId) {
        Sesion s = agenda.cargarConAcceso(sesionId);
        if (!s.esEstado(EstadoSesion.SUSPENDIDA)) {
            throw ApiException.conflict("Solo puede reanudarse una sesión SUSPENDIDA");
        }
        exigirQuorum(s);
        s.setEstado(EstadoSesion.INICIADA);
        auditoria.registrar("EVENT_SESSION_RESUMED", "SESION", s.getId(), null);
        return SesionMapper.aDto(s);
    }

    /** Cierre formal: todos los puntos deben tener decisión (precondición para generar el acta). */
    @Transactional
    public SesionDto finalizar(Long sesionId) {
        UsuarioAutenticado u = SecurityUtils.current();
        Sesion s = agenda.cargarConAcceso(sesionId);
        exigirTransicion(s, EstadoSesion.FINALIZADA);
        List<String> pendientes = s.getPuntos().stream().filter(p -> !p.tieneDecision())
                .map(p -> p.getOrden() + ". " + p.getTitulo()).toList();
        if (!pendientes.isEmpty()) {
            throw ApiException.unprocessable("Hay puntos sin decisión registrada; no puede finalizarse la sesión", pendientes);
        }
        s.setEstado(EstadoSesion.FINALIZADA);
        s.setFinReal(LocalDateTime.now(ZoneId.of(props.getZone())));
        s.getPuntos().stream()
                .filter(p -> p.getSolicitud() != null && p.getSolicitud().getEstado() == EstadoSolicitud.EN_AGENDA)
                .forEach(p -> solicitudService.cambiarEstado(p.getSolicitud(), EstadoSolicitud.PROCESADO,
                        "Procesada en la sesión " + s.getId(), u.nombre()));
        auditoria.registrar("EVENT_SESSION_FINISHED", "SESION", s.getId(), null);
        return SesionMapper.aDto(s);
    }

    // ---------------------------------------------------------------- Decisiones, notas y puntos varios

    /** RF-23. RN-12: corregir una decisión ya asentada exige un motivo y deja huella en la bitácora. */
    @Transactional
    public PuntoDto registrarDecision(Long sesionId, Long puntoId, DecisionRequest req) {
        UsuarioAutenticado u = SecurityUtils.current();
        Sesion s = agenda.cargarConAcceso(sesionId);
        exigirEstado(s, EstadoSesion.INICIADA, "Las decisiones solo se registran con la sesión INICIADA");
        QuorumDto quorum = calcularQuorum(s);
        if (!quorum.alcanzado()) {
            throw ApiException.conflict("Sin quórum deliberatorio: se requieren " + quorum.requeridos()
                    + " integrantes presentes y hay " + quorum.presentes());
        }
        PuntoAgenda p = buscarPunto(s, puntoId);
        int totalVotos = req.votosFavor() + req.votosContra() + req.abstenciones();
        if (totalVotos > quorum.presentes()) {
            throw ApiException.badRequest("El total de votos (" + totalVotos + ") supera la asistencia (" + quorum.presentes() + ")");
        }
        if (p.tieneDecision()) {
            if (req.motivoModificacion() == null || req.motivoModificacion().isBlank()) {
                throw ApiException.badRequest("RN-12: modificar una decisión asentada exige indicar el motivo");
            }
            auditoria.registrar("EVENT_DECISION_MODIFIED", "PUNTO", p.getId(), "Antes: " + p.getResultado() + " ("
                    + p.getVotosFavor() + "/" + p.getVotosContra() + "/" + p.getAbstenciones() + "). Después: "
                    + req.resultado() + " (" + req.votosFavor() + "/" + req.votosContra() + "/" + req.abstenciones()
                    + "). Motivo: " + req.motivoModificacion().trim());
        } else {
            auditoria.registrar("EVENT_DECISION_REGISTERED", "PUNTO", p.getId(), req.resultado().name());
        }
        p.setResultado(req.resultado());
        p.setVotosFavor(req.votosFavor());
        p.setVotosContra(req.votosContra());
        p.setAbstenciones(req.abstenciones());
        p.setDecididoPor(u.nombre());
        return SesionMapper.aPunto(p);
    }

    /** RF-25: agrega una intervención (texto enriquecido básico, sanitizado) a las notas del punto. */
    @Transactional
    public PuntoDto agregarNota(Long sesionId, Long puntoId, NotaRequest req) {
        UsuarioAutenticado u = SecurityUtils.current();
        Sesion s = agenda.cargarConAcceso(sesionId);
        exigirEstado(s, EstadoSesion.INICIADA, "Las notas de debate solo se registran con la sesión INICIADA");
        PuntoAgenda p = buscarPunto(s, puntoId);
        String limpio = HtmlSanitizer.limpiarFragmento(req.contenido());
        if (limpio.isBlank()) {
            throw ApiException.badRequest("La nota no contiene texto válido");
        }
        String nueva = "<div><strong>" + HtmlSanitizer.escapeXml(u.nombre()) + ":</strong> " + limpio + "</div>";
        p.setNotasDebate((p.getNotasDebate() == null ? "" : p.getNotasDebate()) + nueva);
        return SesionMapper.aPunto(p);
    }

    /** RF-26: punto no previsto en el Orden del Día original. */
    @Transactional
    public SesionDto agregarPuntoVario(Long sesionId, PuntoVarioRequest req) {
        Sesion s = agenda.cargarConAcceso(sesionId);
        exigirEstado(s, EstadoSesion.INICIADA, "Los puntos varios solo se crean con la sesión INICIADA");
        PuntoAgenda p = new PuntoAgenda();
        p.setSesion(s);
        p.setOrden(s.getPuntos().size() + 1);
        p.setTitulo(req.titulo().trim());
        p.setDescripcion(req.descripcion() == null || req.descripcion().isBlank() ? null : req.descripcion().trim());
        p.setEsVarios(true);
        s.getPuntos().add(p);
        auditoria.registrar("EVENT_VARIOS_POINT_ADDED", "SESION", s.getId(), p.getTitulo());
        return SesionMapper.aDto(s);
    }

    // ---------------------------------------------------------------- Quórum (también lo usa el generador de actas)

    /** Integrantes activos de la Facultad con derecho a voto. */
    public List<Usuario> integrantes(Sesion s) {
        return usuarios.findActivosPorFacultadYRoles(s.getFacultad().getId(), RolNombre.CON_VOTO);
    }

    public QuorumDto calcularQuorum(Sesion s) {
        List<Usuario> integrantes = integrantes(s);
        Set<Long> ids = integrantes.stream().map(Usuario::getId).collect(Collectors.toSet());
        int presentes = (int) asistencias.findBySesionId(s.getId()).stream()
                .filter(a -> a.isPresente() && ids.contains(a.getUsuario().getId())).count();
        int porcentaje = parametros.getInt(ParametroService.QUORUM_PORCENTAJE, 50);
        return new QuorumDto(integrantes.size(), presentes, QuorumCalculator.requeridos(integrantes.size(), porcentaje),
                porcentaje, QuorumCalculator.alcanzado(integrantes.size(), presentes, porcentaje));
    }

    // ---------------------------------------------------------------- privados

    private AsistenciaDto armarAsistencia(Sesion s) {
        Map<Long, Boolean> presentes = asistencias.findBySesionId(s.getId()).stream()
                .collect(Collectors.toMap(a -> a.getUsuario().getId(), Asistencia::isPresente));
        List<AsistenteDto> asistentes = integrantes(s).stream().map(u -> new AsistenteDto(u.getId(), u.getNombre(),
                u.tieneRol(RolNombre.ROLE_PRESIDENTE) ? "Presidente" : "Consejero", presentes.getOrDefault(u.getId(), false))).toList();
        return new AsistenciaDto(s.getEstado(), calcularQuorum(s), asistentes);
    }

    private PuntoAgenda buscarPunto(Sesion s, Long puntoId) {
        return s.getPuntos().stream().filter(p -> p.getId().equals(puntoId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("El punto no pertenece a esta sesión"));
    }

    private void exigirEstado(Sesion s, EstadoSesion esperado, String mensaje) {
        if (!s.esEstado(esperado)) {
            throw ApiException.conflict(mensaje + " (estado actual: " + s.getEstado() + ")");
        }
    }

    private void exigirTransicion(Sesion s, EstadoSesion destino) {
        if (!s.getEstado().puedePasarA(destino)) {
            throw ApiException.conflict("Transición no permitida: " + s.getEstado() + " → " + destino);
        }
    }

    private void exigirQuorum(Sesion s) {
        QuorumDto q = calcularQuorum(s);
        if (!q.alcanzado()) {
            throw ApiException.conflict("No hay quórum deliberatorio: se requieren " + q.requeridos()
                    + " integrantes presentes y hay " + q.presentes());
        }
    }
}
