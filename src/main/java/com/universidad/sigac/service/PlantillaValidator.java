package com.universidad.sigac.service;

import com.universidad.sigac.util.HtmlSanitizer;
import com.universidad.sigac.util.VariablesCatalogo;
import com.universidad.sigac.util.XhtmlValidator;
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
 * Valida sintaxis y seguridad de plantillas (RN-06, flujo excepcional de CU-02).
 * Política deliberadamente estricta: la plantilla es código que se ejecuta en el servidor, por lo que solo se
 * admiten expresiones de acceso a propiedades (${variable} / ${item.campo}) y un subconjunto de atributos th:*.
 */
@Component
public class PlantillaValidator {

    private static final Pattern EXPR = Pattern.compile("\\$\\{([^}]*)\\}");
    private static final Pattern RUTA = Pattern.compile("^!?\\s*[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*$");
    private static final Pattern EACH = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(?:,\\s*([A-Za-z_][A-Za-z0-9_]*))?\\s*:");
    private static final Pattern INLINE_ESCAPADO = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.DOTALL);
    private static final Pattern UTEXT_NOTAS = Pattern.compile("^\\s*\\$\\{[A-Za-z_][A-Za-z0-9_]*\\.notas\\}\\s*$");
    private static final Set<String> TH_PERMITIDOS =
            Set.of("th:text", "th:utext", "th:each", "th:if", "th:unless", "th:class", "th:classappend");
    private static final Set<String> TOKENS_PROHIBIDOS = Set.of("#{", "*{", "~{", "@{", "__");

    public List<String> validar(String contenido) {
        List<String> errores = new ArrayList<>(XhtmlValidator.errores(contenido));
        if (!errores.isEmpty()) {
            return errores; // sin XHTML válido no es posible seguir analizando
        }
        errores.addAll(HtmlSanitizer.detectarRiesgos(contenido));

        Document doc = Jsoup.parse(contenido, "", Parser.xmlParser());
        Set<String> raices = new HashSet<>(VariablesCatalogo.NOMBRES);
        Set<String> usadas = new HashSet<>();

        // Pasada 1: variables de iteración declaradas.
        for (Element el : doc.getAllElements()) {
            for (Attribute a : el.attributes()) {
                if ("th:each".equals(normalizar(a.getKey()))) {
                    Matcher m = EACH.matcher(a.getValue());
                    if (m.find()) {
                        raices.add(m.group(1));
                        if (m.group(2) != null) {
                            raices.add(m.group(2));
                        }
                    } else {
                        errores.add("th:each con sintaxis inválida: " + a.getValue());
                    }
                }
            }
        }

        // Pasada 2: validar atributos y expresiones.
        for (Element el : doc.getAllElements()) {
            for (Attribute a : el.attributes()) {
                String nombre = normalizar(a.getKey());
                if (!nombre.startsWith("th:")) {
                    continue;
                }
                String valor = a.getValue();
                if (!TH_PERMITIDOS.contains(nombre)) {
                    errores.add("Atributo Thymeleaf no permitido: " + a.getKey()
                            + " (permitidos: th:text, th:utext, th:each, th:if, th:unless, th:class, th:classappend)");
                    continue;
                }
                for (String t : TOKENS_PROHIBIDOS) {
                    if (valor.contains(t)) {
                        errores.add("Expresión no permitida en " + a.getKey() + ": solo se admiten expresiones ${variable}");
                    }
                }
                if ("th:utext".equals(nombre) && !UTEXT_NOTAS.matcher(valor).matches()) {
                    errores.add("th:utext solo se permite para el campo notas de un acuerdo (p. ej. ${ac.notas})");
                }
                Matcher m = EXPR.matcher(valor);
                while (m.find()) {
                    validarExpresion(m.group(1), raices, usadas, errores);
                }
            }
        }

        if (contenido.contains("[(")) {
            errores.add("El inlining sin escape [( ... )] no está permitido; use [[${variable}]]");
        }
        Matcher inl = INLINE_ESCAPADO.matcher(contenido);
        while (inl.find()) {
            String interior = inl.group(1).trim();
            Matcher e = EXPR.matcher(interior);
            if (e.matches()) {
                validarExpresion(e.group(1), raices, usadas, errores);
            } else {
                errores.add("Inlining no permitido: [[" + interior + "]]");
            }
        }

        if (!usadas.contains("consecutivo")) {
            errores.add("La plantilla debe incluir la variable ${consecutivo}");
        }
        if (!usadas.contains("acuerdos")) {
            errores.add("La plantilla debe iterar sobre ${acuerdos}");
        }
        return errores;
    }

    private void validarExpresion(String interior, Set<String> raices, Set<String> usadas, List<String> errores) {
        String e = interior.trim();
        if (!RUTA.matcher(e).matches()) {
            errores.add("Expresión no permitida: ${" + e + "} (solo variables del catálogo o propiedades como ${item.campo})");
            return;
        }
        String[] seg = e.replaceFirst("^!\\s*", "").split("\\.");
        for (String s : seg) {
            String l = s.toLowerCase(Locale.ROOT);
            if (l.equals("class") || l.contains("classloader") || l.equals("getclass") || l.equals("declaringclass")) {
                errores.add("Acceso no permitido en la expresión ${" + e + "}");
                return;
            }
        }
        if (!raices.contains(seg[0])) {
            errores.add("Variable desconocida: ${" + seg[0] + "}. Use el catálogo de variables estandarizadas");
            return;
        }
        usadas.add(seg[0]);
    }

    private String normalizar(String atributo) {
        String l = atributo.toLowerCase(Locale.ROOT);
        return l.startsWith("data-th-") ? "th:" + l.substring(8) : l;
    }
}
