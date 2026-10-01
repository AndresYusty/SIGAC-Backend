package com.universidad.sigac.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Verifica que el contenido sea XHTML bien formado (requisito del motor de PDF). Protegido contra XXE. */
public final class XhtmlValidator {

    private static final Pattern DOCTYPE = Pattern.compile("(?is)<!DOCTYPE[^>]*>");

    private XhtmlValidator() {
    }

    public static List<String> errores(String html) {
        List<String> errores = new ArrayList<>();
        if (html == null || html.isBlank()) {
            errores.add("El contenido no puede estar vacío");
            return errores;
        }
        String contenido = quitarDoctype(html);
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            b.parse(new InputSource(new StringReader(contenido)));
        } catch (SAXParseException e) {
            errores.add("Línea " + e.getLineNumber() + ": " + e.getMessage()
                    + " (el documento debe ser XHTML bien formado: cierre todas las etiquetas y use &#160; en lugar de &nbsp;)");
        } catch (SAXException | IOException | ParserConfigurationException e) {
            errores.add("No fue posible analizar el documento: " + e.getMessage());
        }
        return errores;
    }

    private static String quitarDoctype(String html) {
        Matcher m = DOCTYPE.matcher(html);
        if (m.find()) {
            String saltos = "\n".repeat((int) m.group().chars().filter(c -> c == '\n').count());
            return m.replaceFirst(Matcher.quoteReplacement(saltos));
        }
        return html;
    }
}
