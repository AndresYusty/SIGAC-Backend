package com.university.sigac.modules.agreements.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.sigac.core.pdf.Letterhead;
import com.university.sigac.core.pdf.PdfService;
import com.university.sigac.core.storage.StorageService;
import com.university.sigac.config.AppProperties;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementRequest;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementTypeDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.FieldDto;
import com.university.sigac.modules.agreements.util.AgreementTypeCatalog;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgreementRendererTest {

    private static final Pattern FIELD = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    @TempDir
    Path storage;

    private final AgreementTypeCatalog catalog = new AgreementTypeCatalog(new ObjectMapper());

    @Test
    void fillsFieldsEscapesHtmlAndMarksBold() {
        String html = AgreementRenderer.html("Que **{{studentName}}** pide {{credits}} <b>",
                Map.of("studentName", "ANA & LUIS"));
        assertEquals("Que <strong>ANA &amp; LUIS</strong> pide <span class=\"missing\">credits</span> &lt;b&gt;", html);
    }

    @Test
    void everyTypeDeclaresTheFieldsItUses() {
        for (AgreementTypeDto t : catalog.all()) {
            String all = t.subject() + t.legalBasis() + t.recitals() + t.articles();
            var used = FIELD.matcher(all).results().map(m -> m.group(1)).collect(Collectors.toSet());
            var declared = t.fields().stream().map(FieldDto::key).collect(Collectors.toSet());
            assertEquals(declared, used, "Fields of " + t.code());
        }
    }

    @Test
    void everyTypeCompilesToPdfWithItsExampleValues() {
        AppProperties props = new AppProperties();
        props.getStorage().setBasePath(storage.toString());
        AgreementRenderer renderer = new AgreementRenderer(new PdfService(), new Letterhead(new StorageService(props)));
        for (AgreementTypeDto t : catalog.all()) {
            Map<String, String> values = t.fields().stream()
                    .collect(Collectors.toMap(FieldDto::key, f -> f.example() == null ? "" : f.example()));
            AgreementRequest req = new AgreementRequest(1L, t.code(), 28, LocalDate.of(2026, 1, 27), t.subject(),
                    "EL CONSEJO DE FACULTAD DE INGENIERÍA", t.legalBasis(), t.closing(), "NOMBRE PRESIDENTE",
                    "NOMBRE SECRETARIA", values, t.recitals(), t.articles());
            byte[] pdf = renderer.render(req, 28, "Neiva");
            assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF"), t.code());
        }
    }
}
