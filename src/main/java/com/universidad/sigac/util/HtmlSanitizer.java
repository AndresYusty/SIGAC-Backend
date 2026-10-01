package com.universidad.sigac.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

/**
 * Sanitización de HTML/XHTML que termina convertido a PDF/A. Evita SSRF y lectura de archivos locales
 * (recursos externos), scripts y manejadores de eventos. Solo se admiten imágenes embebidas (data:image/...).
 */
public final class HtmlSanitizer {

    private static final String TAGS_PROHIBIDOS =
            "script,iframe,object,embed,link,base,form,input,button,textarea,select,applet,frame,frameset";
    private static final Pattern CSS_URL_EXTERNA = Pattern.compile("(?i)url\\(\\s*['\"]?\\s*(?!data:)");
    private static final Pattern CSS_IMPORT = Pattern.compile("(?i)@import|expression\\(");

    private HtmlSanitizer() {
    }

    public static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Para notas de debate (texto enriquecido): deja solo formato básico y devuelve XHTML bien formado. */
    public static String limpiarFragmento(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, "", Safelist.basic(), salidaXhtml());
    }

    /** Sanitiza un documento XHTML completo (acta editada). */
    public static String sanitizarDocumento(String html) {
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        doc.outputSettings(salidaXhtml());
        doc.select(TAGS_PROHIBIDOS).remove();
        doc.select("meta[http-equiv]").remove();
        for (Element img : doc.select("img")) {
            if (!img.attr("src").trim().toLowerCase().startsWith("data:image/")) {
                img.remove();
            }
        }
        for (Element el : doc.getAllElements()) {
            for (Attribute a : new ArrayList<>(el.attributes().asList())) {
                if (atributoRiesgoso(a)) {
                    el.removeAttr(a.getKey());
                }
            }
        }
        for (Element st : doc.select("style")) {
            if (cssRiesgoso(st.wholeText())) {
                st.remove();
            }
        }
        return doc.outerHtml();
    }

    /** Lista de problemas de seguridad detectados (para validar plantillas sin modificarlas). */
    public static List<String> detectarRiesgos(String html) {
        List<String> r = new ArrayList<>();
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        for (Element e : doc.select(TAGS_PROHIBIDOS)) {
            r.add("Elemento no permitido: <" + e.tagName() + ">");
        }
        if (!doc.select("meta[http-equiv]").isEmpty()) {
            r.add("No se permite <meta http-equiv>");
        }
        for (Element img : doc.select("img")) {
            if (!img.attr("src").trim().toLowerCase().startsWith("data:image/")) {
                r.add("Solo se permiten imágenes embebidas (data:image/...;base64). Origen no permitido en <img>");
            }
        }
        for (Element el : doc.getAllElements()) {
            for (Attribute a : el.attributes()) {
                if (atributoRiesgoso(a)) {
                    r.add("Atributo no permitido '" + a.getKey() + "' en <" + el.tagName() + ">");
                }
            }
        }
        for (Element st : doc.select("style")) {
            if (cssRiesgoso(st.wholeText())) {
                r.add("CSS con recursos externos (@import / url(...)) no permitido; use data: URI");
            }
        }
        return r;
    }

    /** Agrega al final del body el bloque visual de firma electrónica con huella de auditoría (RN-16). */
    public static String agregarBloqueFirma(String xhtml, List<String> lineas) {
        Document doc = Jsoup.parse(xhtml, "", Parser.xmlParser());
        doc.outputSettings(salidaXhtml());
        Element body = doc.selectFirst("body");
        if (body == null) {
            throw new IllegalStateException("El documento no tiene elemento <body>");
        }
        Element div = body.appendElement("div");
        div.attr("style", "border:1px solid #444;padding:8px;margin-top:24px;font-size:8.5pt;"
                + "font-family:'DejaVu Sans',sans-serif;page-break-inside:avoid;");
        div.appendElement("p").attr("style", "font-weight:bold;margin:0 0 4px 0;")
                .text("FIRMA ELECTRÓNICA CON HUELLA DE AUDITORÍA");
        for (String l : lineas) {
            div.appendElement("p").attr("style", "margin:0 0 2px 0;word-wrap:break-word;").text(l);
        }
        return doc.outerHtml();
    }

    private static boolean atributoRiesgoso(Attribute a) {
        String k = a.getKey().toLowerCase();
        String v = a.getValue() == null ? "" : a.getValue().trim().toLowerCase();
        if (k.startsWith("on")) {
            return true;
        }
        if (k.equals("style")) {
            return cssRiesgoso(v);
        }
        return (k.equals("href") || k.equals("src") || k.equals("xlink:href")) && v.startsWith("javascript:");
    }

    private static boolean cssRiesgoso(String css) {
        return css != null && (CSS_URL_EXTERNA.matcher(css).find() || CSS_IMPORT.matcher(css).find());
    }

    private static Document.OutputSettings salidaXhtml() {
        return new Document.OutputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .prettyPrint(false);
    }
}
