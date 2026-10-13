package com.university.sigac.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.university.sigac.exception.ApiException;
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
 * Turns data into documents: (1) Thymeleaf fills a template stored as text and (2) OpenHTMLtoPDF
 * (on top of Apache PDFBox) generates a PDF/A-1b (ISO 19005, RN-14) with the embedded font.
 */
@Slf4j
@Service
public class PdfService {

    private final TemplateEngine engine = new TemplateEngine();
    private final byte[] colorProfile = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();

    public PdfService() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);
    }

    /** Fills the template with the variables (the template must already have passed the TemplateValidator). */
    public String render(String template, Map<String, Object> variables) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariables(variables);
            return engine.process(template, ctx);
        } catch (RuntimeException e) {
            log.warn("Error processing the template: {}", e.getMessage());
            throw ApiException.unprocessable("The template could not be processed with the supplied data",
                    List.of(String.valueOf(e.getMessage())));
        }
    }

    /** Compiles well-formed XHTML into PDF/A-1b. */
    public byte[] generatePdfA(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B);
            b.useColorProfile(colorProfile);
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
            log.error("Error generating PDF/A", e);
            throw ApiException.unprocessable("Could not generate the PDF/A from the content",
                    List.of(String.valueOf(e.getMessage())));
        }
    }
}
