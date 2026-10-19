package com.university.sigac.modules.agreements.service;

import com.university.sigac.core.pdf.Letterhead;
import com.university.sigac.core.pdf.PdfService;
import com.university.sigac.core.util.HtmlSanitizer;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementRequest;
import com.university.sigac.modules.agreements.dto.AgreementDtos.Block;
import com.university.sigac.modules.agreements.util.SpanishDates;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Fills the agreement layout (sigac/templates/agreement.html) and compiles it to PDF/A. */
@Component
@RequiredArgsConstructor
public class AgreementRenderer {

    private static final String LAYOUT = read("/sigac/templates/agreement.html");
    private static final Pattern FIELD = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");

    private final PdfService pdf;
    private final Letterhead letterhead;

    public byte[] render(AgreementRequest a, int number, String city) {
        Map<String, String> values = a.values() == null ? Map.of() : a.values();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", "ACUERDO No. " + String.format("%03d", number) + " DE " + a.issueDate().getYear());
        m.put("dateLine", SpanishDates.shortLine(a.issueDate()));
        m.put("subject", html(a.subject(), values));
        m.put("councilTitle", html(a.councilTitle(), values));
        m.put("legalBasis", html(a.legalBasis(), values));
        m.put("recitals", blocks(a.recitals(), values));
        m.put("articles", blocks(a.articles(), values));
        m.put("closing", html(a.closing(), values));
        m.put("issuedOn", SpanishDates.issuedOn(city, a.issueDate()));
        m.put("presidentName", text(a.presidentName()));
        m.put("secretaryName", text(a.secretaryName()));
        String xhtml = HtmlSanitizer.sanitizeDocument(pdf.render(letterhead.embed(LAYOUT), m));
        return pdf.generatePdfA(xhtml);
    }

    /** Escapes the text, fills {{field}} (missing ones stay highlighted) and turns **x** into bold. */
    static String html(String text, Map<String, String> values) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher f = FIELD.matcher(HtmlSanitizer.escapeXml(text));
        StringBuilder sb = new StringBuilder();
        while (f.find()) {
            String value = values.get(f.group(1));
            String replacement = value == null || value.isBlank()
                    ? "<span class=\"missing\">" + f.group(1) + "</span>"
                    : HtmlSanitizer.escapeXml(value.trim());
            f.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        f.appendTail(sb);
        return BOLD.matcher(sb.toString()).replaceAll("<strong>$1</strong>").replace("\n", "<br/>");
    }

    private static List<Map<String, Object>> blocks(List<Block> blocks, Map<String, String> values) {
        List<Map<String, Object>> out = new ArrayList<>();
        int article = 0;
        for (Block b : blocks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", b.type());
            if ("table".equals(b.type())) {
                List<String> columns = b.columns() == null ? List.of() : b.columns();
                m.put("columns", columns.stream().map(c -> html(c, values)).toList());
                m.put("rows", (b.rows() == null ? List.<List<String>>of() : b.rows()).stream()
                        .map(r -> r.stream().map(c -> html(c, values)).toList()).toList());
            } else {
                if ("article".equals(b.type())) {
                    m.put("number", ++article);
                }
                m.put("html", html(b.text(), values));
            }
            out.add(m);
        }
        return out;
    }

    private static String text(String s) {
        return s == null ? "" : s.trim();
    }

    private static String read(String path) {
        try (InputStream in = AgreementRenderer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
