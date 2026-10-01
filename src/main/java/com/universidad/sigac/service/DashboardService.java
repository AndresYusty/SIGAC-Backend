package com.universidad.sigac.service;

import com.universidad.sigac.dto.ActaDtos.DashboardDto;
import com.universidad.sigac.entity.Acta;
import com.universidad.sigac.enums.EstadoActa;
import com.universidad.sigac.enums.EstadoSesion;
import com.universidad.sigac.enums.EstadoSolicitud;
import com.universidad.sigac.repository.ActaRepository;
import com.universidad.sigac.repository.SesionRepository;
import com.universidad.sigac.repository.SolicitudRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicadores sencillos según el rol del usuario. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SolicitudRepository solicitudes;
    private final SesionRepository sesiones;
    private final ActaRepository actas;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public DashboardDto obtener() {
        UsuarioAutenticado u = SecurityUtils.current();
        Long fac = u.facultadId();
        Map<String, Object> ind = new LinkedHashMap<>();

        if (u.tieneRol("ROLE_SECRETARIA_2")) {
            Map<String, Long> porEstado = new LinkedHashMap<>();
            for (EstadoSolicitud e : EstadoSolicitud.values()) {
                porEstado.put(e.name(), solicitudes.countByRadicadorIdAndEstado(u.id(), e));
            }
            ind.put("misSolicitudesPorEstado", porEstado);
        }
        if (u.tieneRol("ROLE_SECRETARIA_1") && fac != null) {
            ind.put("solicitudesPendientesDeEvaluacion", solicitudes.countByFacultadIdAndEstado(fac, EstadoSolicitud.RADICADO));
            ind.put("solicitudesAprobadasSinAgenda", solicitudes.countByFacultadIdAndEstado(fac, EstadoSolicitud.APROBADO));
            ind.put("sesionesProximas", sesiones.countByFacultadIdAndEstadoIn(fac, List.of(EstadoSesion.PROGRAMADA, EstadoSesion.CONVOCADA)));
            ind.put("actasEnElaboracion", actas.countByFacultadIdAndEstadoIn(fac, List.of(EstadoActa.BORRADOR, EstadoActa.EN_EDICION)));
        }
        if (u.tieneAlgunRol("ROLE_SECRETARIA_1", "ROLE_PRESIDENTE", "ROLE_CONSEJERO") && fac != null) {
            ind.put("actasPendientesDeFirma", actas.countByFacultadIdAndEstadoIn(fac, List.of(EstadoActa.PENDIENTE_DE_FIRMA)));
            ind.put("sesionesFinalizadas", sesiones.countByFacultadIdAndEstadoIn(fac, List.of(EstadoSesion.FINALIZADA)));
            List<Map<String, Object>> ultimas = new ArrayList<>();
            for (Acta a : actas.findTop5ByFacultadIdAndEstadoOrderByFirmadoEnDesc(fac, EstadoActa.PUBLICADA)) {
                ultimas.add(Map.of("id", a.getId(), "consecutivo", a.getConsecutivo(), "fechaSesion", a.getFechaSesion()));
            }
            ind.put("ultimasActasPublicadas", ultimas);
        }
        if (u.tieneRol("ROLE_ADMIN")) {
            ind.put("usuariosActivos", usuarios.countByActivo(true));
            ind.put("usuariosInactivos", usuarios.countByActivo(false));
            ind.put("actasPublicadasTotal", actas.countByEstado(EstadoActa.PUBLICADA));
        }
        return new DashboardDto(u.nombre(), ind);
    }
}
