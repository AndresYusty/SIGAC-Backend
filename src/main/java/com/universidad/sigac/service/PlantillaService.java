package com.universidad.sigac.service;

import com.universidad.sigac.dto.PlantillaDtos.GuardarPlantillaRequest;
import com.universidad.sigac.dto.PlantillaDtos.PlantillaDto;
import com.universidad.sigac.entity.Facultad;
import com.universidad.sigac.entity.PlantillaActa;
import com.universidad.sigac.entity.TipoConsejo;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.FacultadRepository;
import com.universidad.sigac.repository.PlantillaActaRepository;
import com.universidad.sigac.repository.TipoConsejoRepository;
import com.universidad.sigac.security.SecurityUtils;
import com.universidad.sigac.util.HtmlSanitizer;
import com.universidad.sigac.util.MockDataFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Módulo 2 (RF-10 a RF-14): guardar, versionar, activar y previsualizar plantillas de actas. */
@Service
@RequiredArgsConstructor
public class PlantillaService {

    private final PlantillaActaRepository plantillas;
    private final TipoConsejoRepository tiposConsejo;
    private final FacultadRepository facultades;
    private final PlantillaValidator validator;
    private final PdfService pdf;
    private final AuditService auditoria;

    @Transactional(readOnly = true)
    public List<PlantillaDto> listar(Long tipoConsejoId) {
        List<PlantillaActa> lista = tipoConsejoId == null ? plantillas.findAllByOrderByIdDesc()
                : plantillas.findByTipoConsejoIdOrderByVersionDesc(tipoConsejoId);
        return lista.stream().map(p -> aDto(p, false)).toList();
    }

    @Transactional(readOnly = true)
    public PlantillaDto obtener(Long id) {
        return aDto(buscar(id), true);
    }

    public List<String> validar(String contenido) {
        return validator.validar(contenido);
    }

    /** Guarda una NUEVA versión y la deja activa para su alcance (sin tocar la base de datos a mano, RN-07). */
    @Transactional
    public PlantillaDto guardar(GuardarPlantillaRequest req) {
        TipoConsejo tipo = tiposConsejo.findById(req.tipoConsejoId())
                .orElseThrow(() -> ApiException.notFound("Tipo de consejo no encontrado"));
        Facultad facultad = req.facultadId() == null ? null
                : facultades.findById(req.facultadId()).orElseThrow(() -> ApiException.notFound("Facultad no encontrada"));
        exigirValida(req.contenido());

        Optional<PlantillaActa> ultima = facultad == null
                ? plantillas.findTopByTipoConsejoIdAndFacultadIsNullOrderByVersionDesc(tipo.getId())
                : plantillas.findTopByTipoConsejoIdAndFacultadIdOrderByVersionDesc(tipo.getId(), facultad.getId());
        desactivarVigentes(tipo.getId(), req.facultadId());

        PlantillaActa p = new PlantillaActa();
        p.setTipoConsejo(tipo);
        p.setFacultad(facultad);
        p.setVersion(ultima.map(x -> x.getVersion() + 1).orElse(1));
        p.setNombre(req.nombre() == null || req.nombre().isBlank() ? "Plantilla oficial - " + tipo.getNombre() : req.nombre().trim());
        p.setContenido(req.contenido());
        p.setActiva(true);
        p.setComentario(req.comentario());
        p.setCreadoPor(SecurityUtils.current().email());
        plantillas.save(p);
        auditoria.registrar("EVENT_TEMPLATE_SAVED", "PLANTILLA", p.getId(), "Versión " + p.getVersion());
        return aDto(p, true);
    }

    /** Reactiva una versión anterior. */
    @Transactional
    public PlantillaDto activar(Long id) {
        PlantillaActa p = buscar(id);
        desactivarVigentes(p.getTipoConsejo().getId(), p.getFacultad() == null ? null : p.getFacultad().getId());
        p.setActiva(true);
        auditoria.registrar("EVENT_TEMPLATE_ACTIVATED", "PLANTILLA", p.getId(), "Versión " + p.getVersion());
        return aDto(p, true);
    }

    /** RF-12: renderiza con datos de ejemplo y devuelve el PDF/A, sin guardar nada. */
    public byte[] previsualizar(String contenido) {
        exigirValida(contenido);
        String html = pdf.renderizar(contenido, MockDataFactory.crear());
        return pdf.generarPdfA(HtmlSanitizer.sanitizarDocumento(html));
    }

    /** Plantilla activa de la Facultad o, si no tiene una propia, la general del tipo de consejo. */
    @Transactional(readOnly = true)
    public PlantillaActa obtenerActiva(Long tipoConsejoId, Long facultadId) {
        Optional<PlantillaActa> propia = plantillas.findByTipoConsejoIdAndFacultadIdAndActivaTrue(tipoConsejoId, facultadId)
                .stream().findFirst();
        return propia.or(() -> plantillas.findByTipoConsejoIdAndFacultadIsNullAndActivaTrue(tipoConsejoId).stream().findFirst())
                .orElseThrow(() -> ApiException.conflict(
                        "No hay una plantilla activa para este tipo de consejo. Pida al perfil técnico (ROLE_DEV) publicarla"));
    }

    /** Carga inicial: crea la plantilla por defecto si el tipo de consejo aún no tiene ninguna. */
    @Transactional
    public void crearPorDefectoSiNoExiste(TipoConsejo tipo) {
        if (plantillas.findTopByTipoConsejoIdAndFacultadIsNullOrderByVersionDesc(tipo.getId()).isPresent()) {
            return;
        }
        PlantillaActa p = new PlantillaActa();
        p.setTipoConsejo(tipo);
        p.setNombre("Plantilla oficial - " + tipo.getNombre());
        p.setVersion(1);
        p.setContenido(leerPlantillaPorDefecto());
        p.setActiva(true);
        p.setComentario("Plantilla inicial del sistema");
        p.setCreadoPor("sistema");
        plantillas.save(p);
    }

    // ---------------------------------------------------------------- privados

    private void desactivarVigentes(Long tipoConsejoId, Long facultadId) {
        List<PlantillaActa> vigentes = facultadId == null
                ? plantillas.findByTipoConsejoIdAndFacultadIsNullAndActivaTrue(tipoConsejoId)
                : plantillas.findByTipoConsejoIdAndFacultadIdAndActivaTrue(tipoConsejoId, facultadId);
        vigentes.forEach(v -> v.setActiva(false));
        plantillas.saveAllAndFlush(vigentes);
    }

    private void exigirValida(String contenido) {
        List<String> errores = validator.validar(contenido);
        if (!errores.isEmpty()) {
            throw ApiException.unprocessable("La plantilla contiene errores y no puede usarse", errores);
        }
    }

    private PlantillaActa buscar(Long id) {
        return plantillas.findById(id).orElseThrow(() -> ApiException.notFound("Plantilla no encontrada"));
    }

    private String leerPlantillaPorDefecto() {
        try (InputStream in = PlantillaService.class.getResourceAsStream("/sigac/plantillas/acta-default.html")) {
            if (in == null) {
                throw new IllegalStateException("Falta el recurso /sigac/plantillas/acta-default.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible leer la plantilla por defecto", e);
        }
    }

    private PlantillaDto aDto(PlantillaActa p, boolean conContenido) {
        Facultad f = p.getFacultad();
        return new PlantillaDto(p.getId(), p.getNombre(), p.getTipoConsejo().getId(), p.getTipoConsejo().getNombre(),
                f == null ? null : f.getId(), f == null ? null : f.getNombre(), p.getVersion(), p.isActiva(),
                p.getComentario(), p.getCreadoPor(), p.getCreadoEn(), conContenido ? p.getContenido() : null);
    }
}
