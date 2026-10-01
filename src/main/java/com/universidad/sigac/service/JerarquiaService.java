package com.universidad.sigac.service;

import com.universidad.sigac.dto.ConfigDtos.FacultadRequest;
import com.universidad.sigac.dto.ConfigDtos.NodoDto;
import com.universidad.sigac.dto.ConfigDtos.ProgramaRequest;
import com.universidad.sigac.dto.ConfigDtos.SedeRequest;
import com.universidad.sigac.dto.ConfigDtos.UniversidadRequest;
import com.universidad.sigac.entity.Facultad;
import com.universidad.sigac.entity.Programa;
import com.universidad.sigac.entity.Sede;
import com.universidad.sigac.entity.Universidad;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.FacultadRepository;
import com.universidad.sigac.repository.ProgramaRepository;
import com.universidad.sigac.repository.SedeRepository;
import com.universidad.sigac.repository.UniversidadRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-03 / RF-07: jerarquía institucional Universidad > Sede > Facultad > Programa. */
@Service
@RequiredArgsConstructor
@Transactional
public class JerarquiaService {

    private final UniversidadRepository universidades;
    private final SedeRepository sedes;
    private final FacultadRepository facultades;
    private final ProgramaRepository programas;

    // ---------------- Universidades
    @Transactional(readOnly = true)
    public List<NodoDto> listarUniversidades() {
        return universidades.findAll().stream().map(u -> new NodoDto(u.getId(), u.getNombre(), null, null)).toList();
    }

    public NodoDto crearUniversidad(UniversidadRequest r) {
        if (universidades.existsByNombreIgnoreCase(r.nombre().trim())) {
            throw ApiException.conflict("Ya existe una universidad con ese nombre");
        }
        Universidad u = new Universidad();
        u.setNombre(r.nombre().trim());
        universidades.save(u);
        return new NodoDto(u.getId(), u.getNombre(), null, null);
    }

    public NodoDto editarUniversidad(Long id, UniversidadRequest r) {
        Universidad u = universidades.findById(id).orElseThrow(() -> ApiException.notFound("Universidad no encontrada"));
        u.setNombre(r.nombre().trim());
        return new NodoDto(u.getId(), u.getNombre(), null, null);
    }

    // ---------------- Sedes
    @Transactional(readOnly = true)
    public List<NodoDto> listarSedes(Long universidadId) {
        return sedes.findByUniversidadIdOrderByNombre(universidadId).stream().map(this::aDto).toList();
    }

    public NodoDto crearSede(SedeRequest r) {
        Sede s = new Sede();
        s.setUniversidad(universidades.findById(r.universidadId()).orElseThrow(() -> ApiException.notFound("Universidad no encontrada")));
        s.setNombre(r.nombre().trim());
        s.setCiudad(r.ciudad());
        return aDto(sedes.save(s));
    }

    public NodoDto editarSede(Long id, SedeRequest r) {
        Sede s = sedes.findById(id).orElseThrow(() -> ApiException.notFound("Sede no encontrada"));
        s.setNombre(r.nombre().trim());
        s.setCiudad(r.ciudad());
        return aDto(s);
    }

    // ---------------- Facultades
    @Transactional(readOnly = true)
    public List<NodoDto> listarFacultades(Long sedeId) {
        return facultades.findBySedeIdOrderByNombre(sedeId).stream().map(this::aDto).toList();
    }

    public NodoDto crearFacultad(FacultadRequest r) {
        Facultad f = new Facultad();
        f.setSede(sedes.findById(r.sedeId()).orElseThrow(() -> ApiException.notFound("Sede no encontrada")));
        f.setNombre(r.nombre().trim());
        return aDto(facultades.save(f));
    }

    public NodoDto editarFacultad(Long id, FacultadRequest r) {
        Facultad f = facultades.findById(id).orElseThrow(() -> ApiException.notFound("Facultad no encontrada"));
        f.setNombre(r.nombre().trim());
        return aDto(f);
    }

    // ---------------- Programas
    @Transactional(readOnly = true)
    public List<NodoDto> listarProgramas(Long facultadId) {
        return programas.findByFacultadIdOrderByNombre(facultadId).stream().map(this::aDto).toList();
    }

    public NodoDto crearPrograma(ProgramaRequest r) {
        Programa p = new Programa();
        p.setFacultad(facultades.findById(r.facultadId()).orElseThrow(() -> ApiException.notFound("Facultad no encontrada")));
        p.setNombre(r.nombre().trim());
        return aDto(programas.save(p));
    }

    public NodoDto editarPrograma(Long id, ProgramaRequest r) {
        Programa p = programas.findById(id).orElseThrow(() -> ApiException.notFound("Programa no encontrado"));
        p.setNombre(r.nombre().trim());
        return aDto(p);
    }

    private NodoDto aDto(Sede s) {
        return new NodoDto(s.getId(), s.getNombre(), s.getUniversidad().getId(), s.getCiudad());
    }

    private NodoDto aDto(Facultad f) {
        return new NodoDto(f.getId(), f.getNombre(), f.getSede().getId(), null);
    }

    private NodoDto aDto(Programa p) {
        return new NodoDto(p.getId(), p.getNombre(), p.getFacultad().getId(), null);
    }
}
