package com.university.sigac.core.util;

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
 * Sanitization of HTML/XHTML that ends up converted to PDF/A. Prevents SSRF and local file reads
 * (external resources), scripts and event handlers. Only embedded images (data:image/...) are allowed.
 */
public final class HtmlSanitizer {

    private static final String FORBIDDEN_TAGS =
            "script,iframe,object,embed,link,base,form,input,button,textarea,select,applet,frame,frameset";
    private static final Pattern CSS_EXTERNAL_URL = Pattern.compile("(?i)url\\(\\s*['\"]?\\s*(?!data:)");
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

    /** For debate notes (rich text): keeps only basic formatting and returns well-formed XHTML. */
    public static String cleanFragment(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, "", Safelist.basic(), xhtmlOutput());
    }

    /** Sanitizes a complete XHTML document (edited minutes). */
    public static String sanitizeDocument(String html) {
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        doc.outputSettings(xhtmlOutput());
        doc.select(FORBIDDEN_TAGS).remove();
        doc.select("meta[http-equiv]").remove();
        for (Element img : doc.select("img")) {
            if (!img.attr("src").trim().toLowerCase().startsWith("data:image/")) {
                img.remove();
            }
        }
        for (Element el : doc.getAllElements()) {
            for (Attribute a : new ArrayList<>(el.attributes().asList())) {
                if (isRiskyAttribute(a)) {
                    el.removeAttr(a.getKey());
                }
            }
        }
        for (Element st : doc.select("style")) {
            if (isRiskyCss(st.wholeText())) {
                st.remove();
            }
        }
        return doc.outerHtml();
    }

    /** List of detected security issues (to validate templates without modifying them). */
    public static List<String> detectRisks(String html) {
        List<String> r = new ArrayList<>();
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        for (Element e : doc.select(FORBIDDEN_TAGS)) {
            r.add("Element not allowed: <" + e.tagName() + ">");
        }
        if (!doc.select("meta[http-equiv]").isEmpty()) {
            r.add("<meta http-equiv> is not allowed");
        }
        for (Element img : doc.select("img")) {
            String src = img.attr("src").trim();
            if (!src.toLowerCase().startsWith("data:image/") && !src.equals("letterhead:usco")) {
                r.add("Only embedded images (data:image/...;base64) are allowed. Source not allowed in <img>");
            }
        }
        for (Element el : doc.getAllElements()) {
            for (Attribute a : el.attributes()) {
                if (isRiskyAttribute(a)) {
                    r.add("Attribute not allowed '" + a.getKey() + "' in <" + el.tagName() + ">");
                }
            }
        }
        for (Element st : doc.select("style")) {
            if (isRiskyCss(st.wholeText())) {
                r.add("CSS with external resources (@import / url(...)) is not allowed; use a data: URI");
            }
        }
        return r;
    }

    /** Appends the visual electronic signature block with audit fingerprint at the end of the body (RN-16). */
    public static String appendSignatureBlock(String xhtml, List<String> lines) {
        Document doc = Jsoup.parse(xhtml, "", Parser.xmlParser());
        doc.outputSettings(xhtmlOutput());
        Element body = doc.selectFirst("body");
        if (body == null) {
            throw new IllegalStateException("The document has no <body> element");
        }
        Element div = body.appendElement("div");
        div.attr("style", "border:1px solid #444;padding:8px;margin-top:24px;font-size:8.5pt;"
                + "font-family:'DejaVu Sans',sans-serif;page-break-inside:avoid;");
        div.appendElement("p").attr("style", "font-weight:bold;margin:0 0 4px 0;")
                .text("ELECTRONIC SIGNATURE WITH AUDIT FINGERPRINT");
        for (String l : lines) {
            div.appendElement("p").attr("style", "margin:0 0 2px 0;word-wrap:break-word;").text(l);
        }
        return doc.outerHtml();
    }

    private static boolean isRiskyAttribute(Attribute a) {
        String k = a.getKey().toLowerCase();
        String v = a.getValue() == null ? "" : a.getValue().trim().toLowerCase();
        if (k.startsWith("on")) {
            return true;
        }
        if (k.equals("style")) {
            return isRiskyCss(v);
        }
        return (k.equals("href") || k.equals("src") || k.equals("xlink:href")) && v.startsWith("javascript:");
    }

    private static boolean isRiskyCss(String css) {
        return css != null && (CSS_EXTERNAL_URL.matcher(css).find() || CSS_IMPORT.matcher(css).find());
    }

    private static Document.OutputSettings xhtmlOutput() {
        return new Document.OutputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .prettyPrint(false);
    }
}
