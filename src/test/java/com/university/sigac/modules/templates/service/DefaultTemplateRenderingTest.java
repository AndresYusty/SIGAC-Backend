package com.university.sigac.modules.templates.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.university.sigac.core.pdf.PdfService;
import com.university.sigac.modules.templates.util.MockDataFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultTemplateRenderingTest {

    private final PdfService pdf = new PdfService();

    private String defaultTemplate() throws Exception {
        try (var in = getClass().getResourceAsStream("/sigac/templates/minutes-default.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void rendersTheDefaultTemplateIntoPdf() throws Exception {
        String html = pdf.render(defaultTemplate(), MockDataFactory.create());
        assertTrue(html.contains("MIN-CO-2026-001"));
        assertTrue(html.contains("Approval of course equivalences"));
        assertTrue(html.contains("Diana Ortiz"));
        byte[] file = pdf.generatePdfA(html);
        assertTrue(new String(file, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF"));
    }

    @Test
    void omitsTheGuestsSectionWhenThereAreNoGuests() throws Exception {
        Map<String, Object> data = MockDataFactory.create();
        data.put("guests", List.of());
        data.put("hasGuests", false);
        String html = pdf.render(defaultTemplate(), data);
        assertFalse(html.contains("Invitados"));
    }
}
