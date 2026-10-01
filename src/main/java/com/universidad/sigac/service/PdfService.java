package com.universidad.sigac.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.universidad.sigac.exception.ApiException;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Convierte datos en documentos: (1) Thymeleaf rellena una plantilla guardada como texto y (2) OpenHTMLtoPDF
 * (sobre Apache PDFBox) genera un PDF/A-1b (ISO 19005, RN-14) con la fuente incrustada.
 */
@Slf4j
@Service
public class PdfService {

    private final TemplateEngine engine = new TemplateEngine();
    private final byte[] perfilColor = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();

    public PdfService() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);
    }

    /** Rellena la plantilla con las variables (la plantilla ya debe haber pasado el PlantillaValidator). */
    public String renderizar(String plantilla, Map<String, Object> variables) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("es-CO"));
            ctx.setVariables(variables);
            return engine.process(plantilla, ctx);
        } catch (RuntimeException e) {
            log.warn("Error procesando la plantilla: {}", e.getMessage());
            throw ApiException.unprocessable("La plantilla no pudo procesarse con los datos suministrados",
                    List.of(String.valueOf(e.getMessage())));
        }
    }

    /** Compila XHTML bien formado a PDF/A-1b. */
    public byte[] generarPdfA(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B);
            b.useColorProfile(perfilColor);
            b.useFont(() -> PdfService.class.getResourceAsStream("/fonts/DejaVuSans.ttf"),
                    "DejaVu Sans", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            b.useFont(() -> PdfService.class.getResourceAsStream("/fonts/DejaVuSans-Bold.ttf"),
                    "DejaVu Sans", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            b.withProducer("SIGAC");
            b.withHtmlContent(xhtml, null);
            b.toStream(out);
            b.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF/A", e);
            throw ApiException.unprocessable("No fue posible generar el PDF/A a partir del contenido",
                    List.of(String.valueOf(e.getMessage())));
        }
    }
}
