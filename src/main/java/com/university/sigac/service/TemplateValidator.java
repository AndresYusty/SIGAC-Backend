package com.university.sigac.service;

import com.university.sigac.util.HtmlSanitizer;
import com.university.sigac.util.TemplateVariableCatalog;
import com.university.sigac.util.XhtmlValidator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

/**
 * Validates template syntax and security (RN-06, exceptional flow of UC-02).
 * Deliberately strict policy: the template is code executed on the server, so only property access
 * expressions (${variable} / ${item.field}) and a subset of th:* attributes are allowed.
 */
@Component
public class TemplateValidator {

    private static final Pattern EXPR = Pattern.compile("\\$\\{([^}]*)\\}");
    private static final Pattern PATH = Pattern.compile("^!?\\s*[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*$");
    private static final Pattern EACH = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(?:,\\s*([A-Za-z_][A-Za-z0-9_]*))?\\s*:");
    private static final Pattern ESCAPED_INLINE = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.DOTALL);
    private static final Pattern UTEXT_NOTES = Pattern.compile("^\\s*\\$\\{[A-Za-z_][A-Za-z0-9_]*\\.notes\\}\\s*$");
    private static final Set<String> ALLOWED_TH =
            Set.of("th:text", "th:utext", "th:each", "th:if", "th:unless", "th:class", "th:classappend");
    private static final Set<String> FORBIDDEN_TOKENS = Set.of("#{", "*{", "~{", "@{", "__");

    public List<String> validate(String content) {
        List<String> errors = new ArrayList<>(XhtmlValidator.errors(content));
        if (!errors.isEmpty()) {
            return errors; // without valid XHTML the analysis cannot continue
        }
        errors.addAll(HtmlSanitizer.detectRisks(content));

        Document doc = Jsoup.parse(content, "", Parser.xmlParser());
        Set<String> roots = new HashSet<>(TemplateVariableCatalog.NAMES);
        Set<String> used = new HashSet<>();

        // Pass 1: declared iteration variables.
        for (Element el : doc.getAllElements()) {
            for (Attribute a : el.attributes()) {
                if ("th:each".equals(normalize(a.getKey()))) {
                    Matcher m = EACH.matcher(a.getValue());
                    if (m.find()) {
                        roots.add(m.group(1));
                        if (m.group(2) != null) {
                            roots.add(m.group(2));
                        }
                    } else {
                        errors.add("th:each with invalid syntax: " + a.getValue());
                    }
                }
            }
        }

        // Pass 2: validate attributes and expressions.
        for (Element el : doc.getAllElements()) {
            for (Attribute a : el.attributes()) {
                String name = normalize(a.getKey());
                if (!name.startsWith("th:")) {
                    continue;
                }
                String value = a.getValue();
                if (!ALLOWED_TH.contains(name)) {
                    errors.add("Thymeleaf attribute not allowed: " + a.getKey()
                            + " (allowed: th:text, th:utext, th:each, th:if, th:unless, th:class, th:classappend)");
                    continue;
                }
                for (String t : FORBIDDEN_TOKENS) {
                    if (value.contains(t)) {
                        errors.add("Expression not allowed in " + a.getKey() + ": only ${variable} expressions are supported");
                    }
                }
                if ("th:utext".equals(name) && !UTEXT_NOTES.matcher(value).matches()) {
                    errors.add("th:utext is only allowed for the notes field of an agreement (e.g. ${ag.notes})");
                }
                Matcher m = EXPR.matcher(value);
                while (m.find()) {
                    validateExpression(m.group(1), roots, used, errors);
                }
            }
        }

        if (content.contains("[(")) {
            errors.add("Unescaped inlining [( ... )] is not allowed; use [[${variable}]]");
        }
        Matcher inl = ESCAPED_INLINE.matcher(content);
        while (inl.find()) {
            String inner = inl.group(1).trim();
            Matcher e = EXPR.matcher(inner);
            if (e.matches()) {
                validateExpression(e.group(1), roots, used, errors);
            } else {
                errors.add("Inlining not allowed: [[" + inner + "]]");
            }
        }

        if (!used.contains("serialNumber")) {
            errors.add("The template must include the ${serialNumber} variable");
        }
        if (!used.contains("agreements")) {
            errors.add("The template must iterate over ${agreements}");
        }
        return errors;
    }

    private void validateExpression(String inner, Set<String> roots, Set<String> used, List<String> errors) {
        String e = inner.trim();
        if (!PATH.matcher(e).matches()) {
            errors.add("Expression not allowed: ${" + e + "} (only catalog variables or properties such as ${item.field})");
            return;
        }
        String[] seg = e.replaceFirst("^!\\s*", "").split("\\.");
        for (String s : seg) {
            String l = s.toLowerCase(Locale.ROOT);
            if (l.equals("class") || l.contains("classloader") || l.equals("getclass") || l.equals("declaringclass")) {
                errors.add("Access not allowed in the expression ${" + e + "}");
                return;
            }
        }
        if (!roots.contains(seg[0])) {
            errors.add("Unknown variable: ${" + seg[0] + "}. Use the catalog of standardized variables");
            return;
        }
        used.add(seg[0]);
    }

    private String normalize(String attribute) {
        String l = attribute.toLowerCase(Locale.ROOT);
        return l.startsWith("data-th-") ? "th:" + l.substring(8) : l;
    }
}
