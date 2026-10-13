package com.university.sigac.core.util;

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

/** Checks that the content is well-formed XHTML (required by the PDF engine). Protected against XXE. */
public final class XhtmlValidator {

    private static final Pattern DOCTYPE = Pattern.compile("(?is)<!DOCTYPE[^>]*>");

    private XhtmlValidator() {
    }

    public static List<String> errors(String html) {
        List<String> errors = new ArrayList<>();
        if (html == null || html.isBlank()) {
            errors.add("The content cannot be empty");
            return errors;
        }
        String content = stripDoctype(html);
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
            b.parse(new InputSource(new StringReader(content)));
        } catch (SAXParseException e) {
            errors.add("Line " + e.getLineNumber() + ": " + e.getMessage()
                    + " (the document must be well-formed XHTML: close every tag and use &#160; instead of &nbsp;)");
        } catch (SAXException | IOException | ParserConfigurationException e) {
            errors.add("Could not parse the document: " + e.getMessage());
        }
        return errors;
    }

    private static String stripDoctype(String html) {
        Matcher m = DOCTYPE.matcher(html);
        if (m.find()) {
            String newlines = "\n".repeat((int) m.group().chars().filter(c -> c == '\n').count());
            return m.replaceFirst(Matcher.quoteReplacement(newlines));
        }
        return html;
    }
}
