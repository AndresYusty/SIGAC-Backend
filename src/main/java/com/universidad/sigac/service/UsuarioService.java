package com.universidad.sigac.service;

import com.universidad.sigac.dto.UsuarioDtos.CambioClaveRequest;
import com.universidad.sigac.dto.UsuarioDtos.UsuarioActualizarRequest;
import com.universidad.sigac.dto.UsuarioDtos.UsuarioCrearRequest;
import com.universidad.sigac.dto.UsuarioDtos.UsuarioDto;
import com.universidad.sigac.entity.Facultad;
import com.universidad.sigac.entity.Programa;
import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.enums.RolNombre;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.FacultadRepository;
import com.universidad.sigac.repository.ProgramaRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Módulo 1 – administración de usuarios y roles (RF-04). */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final FacultadRepository facultades;
    private final ProgramaRepository programas;
    private final PasswordEncoder encoder;
    private final AuditService auditoria;

    @Transactional(readOnly = true)
    public Page<UsuarioDto> listar(String q, Pageable pageable) {
        Page<Usuario> page = (q == null || q.isBlank()) ? usuarios.findAll(pageable)
                : usuarios.findByNombreContainingIgnoreCaseOrEmailContainingIgnoreCase(q.trim(), q.trim(), pageable);
        return page.map(UsuarioService::aDto);
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfilActual() {
        return aDto(buscar(SecurityUtils.current().id()));
    }

    @Transactional
    public UsuarioDto crear(UsuarioCrearRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Ya existe un usuario registrado con ese correo");
        }
        validarClave(req.password());
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        asignarRolesYAmbito(u, req.roles(), req.facultadId(), req.programaId());
        usuarios.save(u);
        auditoria.registrar("EVENT_USER_CREATED", "USUARIO", u.getId(), "Roles: " + u.getRoles());
        return aDto(u);
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioActualizarRequest req) {
        Usuario u = buscar(id);
        u.setNombre(req.nombre().trim());
        asignarRolesYAmbito(u, req.roles(), req.facultadId(), req.programaId());
        auditoria.registrar("EVENT_USER_UPDATED", "USUARIO", u.getId(), "Roles: " + u.getRoles());
        return aDto(u);
    }

    /** Inactivación lógica: un usuario nunca se elimina físicamente. */
    @Transactional
    public UsuarioDto cambiarEstado(Long id, boolean activo) {
        Usuario u = buscar(id);
        if (!activo && u.getId().equals(SecurityUtils.current().id())) {
            throw ApiException.badRequest("No puede desactivar su propia cuenta");
        }
        u.setActivo(activo);
        auditoria.registrar(activo ? "EVENT_USER_ACTIVATED" : "EVENT_USER_DEACTIVATED", "USUARIO", u.getId(), null);
        return aDto(u);
    }

    @Transactional
    public void cambiarClavePropia(CambioClaveRequest req) {
        Usuario u = buscar(SecurityUtils.current().id());
        if (!encoder.matches(req.claveActual(), u.getPasswordHash())) {
            throw ApiException.badRequest("La contraseña actual no es correcta");
        }
        validarClave(req.claveNueva());
        u.setPasswordHash(encoder.encode(req.claveNueva()));
        auditoria.registrar("EVENT_PASSWORD_CHANGED", "USUARIO", u.getId(), null);
    }

    /** Mínimo 8 caracteres, con al menos una letra y un número. */
    public static void validarClave(String clave) {
        if (clave == null || clave.length() < 8 || !clave.matches(".*[A-Za-z].*") || !clave.matches(".*\\d.*")) {
            throw ApiException.badRequest("La contraseña debe tener mínimo 8 caracteres e incluir letras y números");
        }
    }

    // ---------------------------------------------------------------- mapeos (usados también por AuthService)

    public static UsuarioDto aDto(Usuario u) {
        Facultad f = u.getFacultad();
        Programa p = u.getPrograma();
        return new UsuarioDto(u.getId(), u.getNombre(), u.getEmail(), u.isActivo(), nombresDeRoles(u),
                f == null ? null : f.getId(), f == null ? null : f.getNombre(),
                p == null ? null : p.getId(), p == null ? null : p.getNombre(), u.getUltimoAcceso());
    }

    public static UsuarioAutenticado aAutenticado(Usuario u) {
        return new UsuarioAutenticado(u.getId(), u.getEmail(), u.getNombre(), nombresDeRoles(u),
                u.getFacultad() == null ? null : u.getFacultad().getId(),
                u.getPrograma() == null ? null : u.getPrograma().getId());
    }

    private static List<String> nombresDeRoles(Usuario u) {
        return u.getRoles().stream().map(Enum::name).sorted().toList();
    }

    // ---------------------------------------------------------------- privados

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    /** RN-01: los roles operativos exigen Facultad; el Programa, si se indica, debe pertenecer a esa Facultad. */
    private void asignarRolesYAmbito(Usuario u, Set<RolNombre> roles, Long facultadId, Long programaId) {
        if (roles.contains(RolNombre.ROLE_DEV) && !SecurityUtils.current().tieneRol("ROLE_DEV")) {
            throw ApiException.forbidden("Solo el perfil técnico puede asignar el rol ROLE_DEV");
        }
        Facultad facultad = facultadId == null ? null
                : facultades.findById(facultadId).orElseThrow(() -> ApiException.notFound("Facultad no encontrada"));
        boolean soloGlobales = roles.stream().allMatch(RolNombre::esGlobal);
        if (!soloGlobales && facultad == null) {
            throw ApiException.badRequest("RN-01: los roles operativos requieren una Facultad");
        }
        Programa programa = null;
        if (programaId != null) {
            programa = programas.findById(programaId).orElseThrow(() -> ApiException.notFound("Programa no encontrado"));
            if (facultad == null || !programa.getFacultad().getId().equals(facultad.getId())) {
                throw ApiException.badRequest("El Programa no pertenece a la Facultad indicada");
            }
        }
        u.setRoles(new HashSet<>(roles));
        u.setFacultad(facultad);
        u.setPrograma(programa);
    }
}
