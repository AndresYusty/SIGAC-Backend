package com.universidad.sigac.service;

import com.universidad.sigac.dto.SesionDtos.PuntoDto;
import com.universidad.sigac.dto.SesionDtos.SesionDto;
import com.universidad.sigac.dto.SesionDtos.SesionResumenDto;
import com.universidad.sigac.entity.PuntoAgenda;
import com.universidad.sigac.entity.Sesion;

/** Convierte entidades de sesión en DTOs. Debe usarse dentro de una transacción (navega relaciones LAZY). */
final class SesionMapper {

    private SesionMapper() {
    }

    static SesionDto aDto(Sesion s) {
        return new SesionDto(s.getId(), s.getTipoConsejo().getId(), s.getTipoConsejo().getNombre(), s.getFacultad().getId(),
                s.getFacultad().getNombre(), s.getFechaProgramada(), s.getLugar(), s.getEstado(), s.isAgendaCerrada(),
                s.getInicioReal(), s.getFinReal(), s.getPuntos().stream().map(SesionMapper::aPunto).toList());
    }

    static SesionResumenDto aResumen(Sesion s) {
        return new SesionResumenDto(s.getId(), s.getTipoConsejo().getNombre(), s.getFacultad().getNombre(),
                s.getFechaProgramada(), s.getLugar(), s.getEstado(), s.isAgendaCerrada(), s.getPuntos().size());
    }

    static PuntoDto aPunto(PuntoAgenda p) {
        return new PuntoDto(p.getId(), p.getOrden(), p.getTitulo(), p.getDescripcion(),
                p.getSolicitud() == null ? null : p.getSolicitud().getCodigo(), p.isEsVarios(), p.getResultado(),
                p.getVotosFavor(), p.getVotosContra(), p.getAbstenciones(), p.getDecididoPor(), p.getNotasDebate());
    }
}
