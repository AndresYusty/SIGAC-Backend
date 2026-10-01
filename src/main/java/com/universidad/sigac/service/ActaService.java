package com.universidad.sigac.service;

import com.universidad.sigac.config.AppProperties;
import com.universidad.sigac.dto.ActaDtos.ActaDto;
import com.universidad.sigac.entity.Acta;
import com.universidad.sigac.entity.PlantillaActa;
import com.universidad.sigac.entity.Sesion;
import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.enums.EstadoActa;
import com.universidad.sigac.enums.EstadoSesion;
import com.universidad.sigac.enums.RolNombre;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.ActaRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.ClientIpResolver;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.security.UsuarioAutenticado;
import com.universidad.sigac.util.FechaUtil;
import com.universidad.sigac.util.HashUtil;
import com.universidad.sigac.util.HtmlSanitizer;
import com.universidad.sigac.util.XhtmlValidator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Módulos 5 y 6: generación del acta, edición del borrador, circuito de aprobación y firma electrónica.
 * Flujo: BORRADOR → EN_EDICION ⇄ PENDIENTE_DE_FIRMA → PUBLICADA (congelada, RN-17).
 */
@Service
@RequiredArgsConstructor
public class ActaService {

    private static final Set<EstadoActa> EDITABLES = Set.of(EstadoActa.BORRADOR, EstadoActa.EN_EDICION);

    private final ActaRepository actas;
    private final UsuarioRepository usuarios;
    private final AgendaService agenda;
    private final PlantillaService plantillas;
    private final ActaModeloBuilder modeloBuilder;
    private final PdfService pdf;
    private final ConsecutivoService consecutivos;
    private final StorageService storage;
    private final ClientIpResolver ipResolver;
    private final AuditService auditoria;
    private final EmailService email;
    private final AppProperties props;

    // ---------------------------------------------------------------- Generar y editar (Secretaría 1)

    /** CU-05: compila el acta de una sesión FINALIZADA con la plantilla activa y le asigna el consecutivo (RN-13). */
    @Transactional
    public ActaDto compilar(Long sesionId) {
        Sesion s = agenda.cargarConAcceso(sesionId);
        if (!s.esEstado(EstadoSesion.FINALIZADA)) {
            throw ApiException.conflict("El acta solo puede generarse con la sesión FINALIZADA (estado actual: " + s.getEstado() + ")");
        }
        if (actas.existsBySesionId(sesionId)) {
            throw ApiException.conflict("Ya existe un acta para esta sesión");
        }
        List<String> pendientes = modeloBuilder.puntosPendientes(s);
        if (!pendientes.isEmpty()) {
            throw ApiException.unprocessable("Información incompleta: hay puntos sin cerrar", pendientes);
        }

        PlantillaActa plantilla = plantillas.obtenerActiva(s.getTipoConsejo().getId(), s.getFacultad().getId());
        LocalDateTime fecha = s.getInicioReal() != null ? s.getInicioReal() : s.getFechaProgramada();
        String codigo = s.getTipoConsejo().getCodigo();
        long numero = consecutivos.siguiente("ACTA-" + codigo + "-" + fecha.getYear());
        String consecutivo = String.format("ACTA-%s-%d-%03d", codigo, fecha.getYear(), numero);

        String html = pdf.renderizar(plantilla.getContenido(), modeloBuilder.construir(s, consecutivo));
        html = HtmlSanitizer.sanitizarDocumento(html);
        pdf.generarPdfA(html); // comprueba desde ya que el documento se puede compilar a PDF/A

        Acta a = new Acta();
        a.setSesion(s);
        a.setConsecutivo(consecutivo);
        a.setTipoConsejo(s.getTipoConsejo());
        a.setFacultad(s.getFacultad());
        a.setFechaSesion(fecha.toLocalDate());
        a.setPlantillaVersion(plantilla.getVersion());
        a.setContenidoHtml(html);
        actas.save(a);
        auditoria.registrar("EVENT_ACTA_COMPILED", "ACTA", a.getId(), consecutivo);
        return aDto(a, true);
    }

    /** RF-28: la Secretaría 1 pule la redacción antes de enviar a firma. */
    @Transactional
    public ActaDto editar(Long id, String contenidoHtml) {
        Acta a = cargar(id);
        exigirEditable(a);
        List<String> errores = XhtmlValidator.errores(contenidoHtml);
        if (!errores.isEmpty()) {
            throw ApiException.unprocessable("El contenido del acta no es XHTML válido", errores);
        }
        String limpio = HtmlSanitizer.sanitizarDocumento(contenidoHtml);
        pdf.generarPdfA(limpio);
        a.setContenidoHtml(limpio);
        a.setEstado(EstadoActa.EN_EDICION);
        auditoria.registrar("EVENT_ACTA_EDITED", "ACTA", a.getId(), a.getConsecutivo());
        return aDto(a, true);
    }

    @Transactional(readOnly = true)
    public byte[] vistaPrevia(Long id) {
        Acta a = cargar(id);
        if (a.getEstado() == EstadoActa.PUBLICADA) {
            throw ApiException.conflict("El acta ya está publicada: consúltela en el Repositorio Digital");
        }
        return pdf.generarPdfA(a.getContenidoHtml());
    }

    @Transactional
    public ActaDto enviarAFirma(Long id) {
        Acta a = cargar(id);
        exigirEditable(a);
        pdf.generarPdfA(a.getContenidoHtml());
        a.setEstado(EstadoActa.PENDIENTE_DE_FIRMA);
        a.setObservacionRechazo(null);
        auditoria.registrar("EVENT_ACTA_SENT_TO_SIGN", "ACTA", a.getId(), a.getConsecutivo());
        return aDto(a, false);
    }

    // ---------------------------------------------------------------- Aprobación (Presidente / Secretaría 1)

    /** RF-35: devuelve el borrador a edición con observaciones. */
    @Transactional
    public ActaDto rechazar(Long id, String observacion) {
        Acta a = cargar(id);
        exigirEstado(a, EstadoActa.PENDIENTE_DE_FIRMA);
        a.setEstado(EstadoActa.EN_EDICION);
        a.setObservacionRechazo(observacion.trim());
        auditoria.registrar("EVENT_ACTA_REJECTED", "ACTA", a.getId(), observacion.trim());
        return aDto(a, false);
    }

    /**
     * CU-06: estampa la huella de auditoría (firmante, hora del servidor, IP, hash SHA-256), genera el PDF/A final,
     * lo guarda en el repositorio con su hash de integridad, congela el acta (RN-17) y notifica por correo (RN-15).
     */
    @Transactional
    public ActaDto aprobarYFirmar(Long id) {
        UsuarioAutenticado u = SecurityUtils.current();
        Acta a = cargar(id);
        exigirEstado(a, EstadoActa.PENDIENTE_DE_FIRMA);

        Instant ahora = Instant.now();
        ZoneId zona = ZoneId.of(props.getZone());
        String ip = ipResolver.current();
        String rol = u.tieneRol("ROLE_PRESIDENTE") ? "Presidente del Consejo" : "Secretaría del Consejo";
        String hashContenido = HashUtil.sha256Hex(a.getContenidoHtml());

        String htmlFinal = HtmlSanitizer.agregarBloqueFirma(a.getContenidoHtml(), List.of(
                "Firmado por: " + u.nombre() + " (" + rol + ")",
                "Fecha y hora oficial del servidor: " + FechaUtil.fechaHoraLarga(ahora, zona) + " (" + zona + ")",
                "Dirección IP de origen: " + ip,
                "Hash SHA-256 del contenido aprobado: " + hashContenido));
        byte[] archivo = pdf.generarPdfA(htmlFinal);

        String ruta = rutaRepositorio(a);
        storage.guardar(ruta, archivo);
        try {
            a.setFirmadoPor(u.nombre());
            a.setRolFirmante(rol);
            a.setFirmadoEn(ahora);
            a.setFirmaIp(ip);
            a.setHashContenido(hashContenido);
            a.setContenidoHtml(htmlFinal);
            a.setRutaArchivo(ruta);
            a.setHashArchivo(HashUtil.sha256Hex(archivo));
            a.setEstado(EstadoActa.PUBLICADA);
            actas.saveAndFlush(a);
        } catch (RuntimeException e) {
            storage.eliminarSilencioso(ruta); // no dejar archivos huérfanos si falla el guardado
            throw e;
        }
        auditoria.registrar("EVENT_ACTA_SIGNED", "ACTA", a.getId(), a.getConsecutivo() + " sha256=" + a.getHashArchivo());

        List<String> destinatarios = usuarios.findActivosPorFacultadYRoles(a.getFacultad().getId(),
                        List.of(RolNombre.ROLE_PRESIDENTE, RolNombre.ROLE_CONSEJERO, RolNombre.ROLE_SECRETARIA_1))
                .stream().map(Usuario::getEmail).distinct().toList();
        email.enviarActaPublicada(destinatarios, a.getConsecutivo(), a.getFacultad().getNombre(), ruta);
        return aDto(a, false);
    }

    // ---------------------------------------------------------------- Consulta

    @Transactional(readOnly = true)
    public ActaDto obtener(Long id) {
        return aDto(cargar(id), true);
    }

    @Transactional(readOnly = true)
    public Page<ActaDto> listar(EstadoActa estado, Pageable pageable) {
        UsuarioAutenticado u = SecurityUtils.current();
        Page<Acta> page;
        if (u.esGlobal()) {
            page = estado == null ? actas.findAll(pageable) : actas.findByEstado(estado, pageable);
        } else {
            page = estado == null ? actas.findByFacultadId(u.facultadId(), pageable)
                    : actas.findByFacultadIdAndEstado(u.facultadId(), estado, pageable);
        }
        return page.map(a -> aDto(a, false));
    }

    // ---------------------------------------------------------------- privados

    /** actas/Universidad/Sede/Facultad/Año/CONSECUTIVO.pdf (jerarquía institucional). */
    private String rutaRepositorio(Acta a) {
        var facultad = a.getFacultad();
        return "actas/" + StorageService.slug(facultad.getSede().getUniversidad().getNombre())
                + "/" + StorageService.slug(facultad.getSede().getNombre())
                + "/" + StorageService.slug(facultad.getNombre())
                + "/" + a.getFechaSesion().getYear()
                + "/" + StorageService.slug(a.getConsecutivo()) + ".pdf";
    }

    private Acta cargar(Long id) {
        Acta a = actas.findById(id).orElseThrow(() -> ApiException.notFound("Acta no encontrada"));
        SecurityUtils.verificarFacultad(SecurityUtils.current(), a.getFacultad().getId());
        return a;
    }

    private void exigirEditable(Acta a) {
        if (!EDITABLES.contains(a.getEstado())) {
            throw ApiException.conflict("El acta no admite modificaciones en estado " + a.getEstado()
                    + (a.getEstado() == EstadoActa.PUBLICADA ? " (RN-17: acta congelada)" : ""));
        }
    }

    private void exigirEstado(Acta a, EstadoActa esperado) {
        if (a.getEstado() != esperado) {
            throw ApiException.conflict("Operación válida solo para actas en estado " + esperado + " (actual: " + a.getEstado() + ")");
        }
    }

    private ActaDto aDto(Acta a, boolean conContenido) {
        return new ActaDto(a.getId(), a.getConsecutivo(), a.getEstado(), a.getSesion().getId(), a.getTipoConsejo().getNombre(),
                a.getFacultad().getNombre(), a.getFechaSesion(), a.getObservacionRechazo(), a.getFirmadoPor(), a.getFirmadoEn(),
                conContenido ? a.getContenidoHtml() : null);
    }
}
