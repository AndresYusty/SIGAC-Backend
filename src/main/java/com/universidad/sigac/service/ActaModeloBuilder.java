package com.universidad.sigac.service;

import com.universidad.sigac.dto.SesionDtos.QuorumDto;
import com.universidad.sigac.entity.Asistencia;
import com.universidad.sigac.entity.PuntoAgenda;
import com.universidad.sigac.entity.Sesion;
import com.universidad.sigac.enums.RolNombre;
import com.universidad.sigac.repository.AsistenciaRepository;
import com.universidad.sigac.util.FechaUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Arma el diccionario de variables del catálogo oficial (consecutivo, fechaSesion, listaAsistentes, acuerdos...)
 * con lo registrado en la sesión. Es lo que Thymeleaf inyecta en la plantilla del acta (RF-27).
 */
@Component
@RequiredArgsConstructor
public class ActaModeloBuilder {

    private final SesionEnVivoService sesionService;
    private final AsistenciaRepository asistencias;

    /** Puntos de la sesión que no tienen decisión registrada. */
    public List<String> puntosPendientes(Sesion s) {
        return s.getPuntos().stream().filter(p -> !p.tieneDecision()).map(p -> p.getOrden() + ". " + p.getTitulo()).toList();
    }

    public Map<String, Object> construir(Sesion s, String consecutivo) {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime referencia = s.getInicioReal() != null ? s.getInicioReal() : s.getFechaProgramada();
        m.put("consecutivo", consecutivo);
        m.put("fechaSesion", FechaUtil.fechaLarga(referencia.toLocalDate()));
        m.put("horaInicio", FechaUtil.hora(s.getInicioReal()));
        m.put("horaFin", FechaUtil.hora(s.getFinReal()));
        m.put("lugar", s.getLugar());
        m.put("tipoConsejo", s.getTipoConsejo().getNombre());
        m.put("universidad", s.getFacultad().getSede().getUniversidad().getNombre());
        m.put("sede", s.getFacultad().getSede().getNombre());
        m.put("facultad", s.getFacultad().getNombre());

        QuorumDto q = sesionService.calcularQuorum(s);
        m.put("quorum", q.presentes() + " de " + q.integrantes() + " integrantes con derecho a voto presentes (mínimo requerido: "
                + q.requeridos() + "). " + (q.alcanzado() ? "Se verificó el quórum deliberatorio." : "No se alcanzó el quórum."));

        Map<Long, Boolean> presentes = asistencias.findBySesionId(s.getId()).stream()
                .collect(Collectors.toMap(a -> a.getUsuario().getId(), Asistencia::isPresente));
        List<Map<String, Object>> asistentes = new ArrayList<>();
        sesionService.integrantes(s).forEach(u -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("nombre", u.getNombre());
            a.put("rol", u.tieneRol(RolNombre.ROLE_PRESIDENTE) ? "Presidente" : "Consejero");
            a.put("presente", presentes.getOrDefault(u.getId(), false));
            asistentes.add(a);
        });
        m.put("listaAsistentes", asistentes);

        List<Map<String, Object>> ordenDelDia = new ArrayList<>();
        List<Map<String, Object>> acuerdos = new ArrayList<>();
        for (PuntoAgenda p : s.getPuntos()) {
            String descripcion = p.getDescripcion() == null || p.getDescripcion().isBlank() ? null : p.getDescripcion();
            if (!p.isEsVarios()) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("numero", p.getOrden());
                o.put("titulo", p.getTitulo());
                o.put("descripcion", descripcion);
                ordenDelDia.add(o);
            }
            Map<String, Object> ac = new LinkedHashMap<>();
            ac.put("numero", p.getOrden());
            ac.put("titulo", p.getTitulo());
            ac.put("descripcion", descripcion);
            ac.put("decision", p.tieneDecision() ? p.getResultado().etiqueta() : "Sin decisión");
            ac.put("votosFavor", p.getVotosFavor());
            ac.put("votosContra", p.getVotosContra());
            ac.put("abstenciones", p.getAbstenciones());
            ac.put("notas", p.getNotasDebate() == null ? "" : p.getNotasDebate()); // XHTML ya sanitizado
            ac.put("esVarios", p.isEsVarios());
            acuerdos.add(ac);
        }
        m.put("ordenDelDia", ordenDelDia);
        m.put("acuerdos", acuerdos);
        return m;
    }
}
