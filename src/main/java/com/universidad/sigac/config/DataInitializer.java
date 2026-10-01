package com.universidad.sigac.config;

import com.universidad.sigac.entity.Facultad;
import com.universidad.sigac.entity.Programa;
import com.universidad.sigac.entity.Sede;
import com.universidad.sigac.entity.TipoConsejo;
import com.universidad.sigac.entity.Universidad;
import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.enums.RolNombre;
import com.universidad.sigac.repository.FacultadRepository;
import com.universidad.sigac.repository.ProgramaRepository;
import com.universidad.sigac.repository.SedeRepository;
import com.universidad.sigac.repository.TipoConsejoRepository;
import com.universidad.sigac.repository.UniversidadRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.service.ParametroService;
import com.universidad.sigac.service.PlantillaService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Carga inicial idempotente: tipos de consejo, parámetros, usuarios técnicos, plantilla por defecto y datos demo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final TipoConsejoRepository tiposConsejo;
    private final ParametroService parametros;
    private final UsuarioRepository usuarios;
    private final UniversidadRepository universidades;
    private final SedeRepository sedes;
    private final FacultadRepository facultades;
    private final ProgramaRepository programas;
    private final PlantillaService plantillas;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearTipoConsejo("CF", "Consejo de Facultad");
        crearTipoConsejo("CA", "Consejo Académico");
        crearTipoConsejo("CS", "Consejo Superior");

        parametros.asegurar(ParametroService.ARCHIVOS_MAX_MB, "10", "Tamaño máximo por archivo adjunto (MB)");
        parametros.asegurar(ParametroService.QUORUM_PORCENTAJE, "50",
                "Porcentaje base del quórum deliberatorio (mínimo = porcentaje de integrantes + 1)");

        crearUsuario("Administrador SIGAC", props.getSeed().getAdminEmail(), props.getSeed().getAdminPassword(), RolNombre.ROLE_ADMIN, null, null);
        crearUsuario("Desarrollador SIGAC", props.getSeed().getDevEmail(), props.getSeed().getDevPassword(), RolNombre.ROLE_DEV, null, null);
        tiposConsejo.findAll().forEach(plantillas::crearPorDefectoSiNoExiste);

        if (props.getSeed().isDemoData() && universidades.count() == 0) {
            cargarDemo();
        }
    }

    private void crearTipoConsejo(String codigo, String nombre) {
        if (tiposConsejo.findByCodigo(codigo).isEmpty()) {
            tiposConsejo.save(new TipoConsejo(codigo, nombre));
        }
    }

    private void crearUsuario(String nombre, String email, String clave, RolNombre rol, Facultad facultad, Programa programa) {
        if (usuarios.existsByEmailIgnoreCase(email)) {
            return;
        }
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(clave));
        u.setRoles(new java.util.HashSet<>(Set.of(rol)));
        u.setFacultad(facultad);
        u.setPrograma(programa);
        usuarios.save(u);
        log.warn("Usuario creado: {} ({}). Cambie la contraseña inicial.", email, rol);
    }

    private void cargarDemo() {
        Universidad uni = new Universidad();
        uni.setNombre("Universidad Demo");
        universidades.save(uni);
        Sede sede = new Sede();
        sede.setNombre("Sede Principal");
        sede.setCiudad("Bogotá");
        sede.setUniversidad(uni);
        sedes.save(sede);
        Facultad fac = new Facultad();
        fac.setNombre("Facultad de Ingeniería");
        fac.setSede(sede);
        facultades.save(fac);
        Programa prog = new Programa();
        prog.setNombre("Ingeniería de Sistemas");
        prog.setFacultad(fac);
        programas.save(prog);

        String clave = "Demo12345*";
        crearUsuario("Secretaría Gestora Demo", "sec1@demo.edu", clave, RolNombre.ROLE_SECRETARIA_1, fac, null);
        crearUsuario("Secretaría Auxiliar Demo", "sec2@demo.edu", clave, RolNombre.ROLE_SECRETARIA_2, fac, prog);
        crearUsuario("Presidente Demo (Decano)", "presidente@demo.edu", clave, RolNombre.ROLE_PRESIDENTE, fac, null);
        crearUsuario("Consejero Uno", "consejero1@demo.edu", clave, RolNombre.ROLE_CONSEJERO, fac, null);
        crearUsuario("Consejero Dos", "consejero2@demo.edu", clave, RolNombre.ROLE_CONSEJERO, fac, null);
        crearUsuario("Consejero Tres", "consejero3@demo.edu", clave, RolNombre.ROLE_CONSEJERO, fac, null);
        log.warn("Datos DEMO cargados (clave de los usuarios demo: {}). No usar en producción.", clave);
    }
}
