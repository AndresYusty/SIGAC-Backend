package com.university.sigac.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    @Test
    void notesKeepBasicFormattingAndRemoveScripts() {
        String clean = HtmlSanitizer.cleanFragment("<p>Hello <b>world</b></p><script>alert(1)</script><img src=\"http://x/y.png\">");
        assertTrue(clean.contains("<b>world</b>"));
        assertFalse(clean.contains("script"));
        assertFalse(clean.contains("img"));
    }

    @Test
    void documentRemovesExternalResourcesButKeepsDataUri() {
        String html = "<html><body><img src=\"http://evil/x.png\"/><img src=\"data:image/png;base64,AAAA\"/>"
                + "<p onclick=\"x()\" style=\"background:url(http://evil/a.png)\">ok</p></body></html>";
        String clean = HtmlSanitizer.sanitizeDocument(html);
        assertFalse(clean.contains("evil"));
        assertFalse(clean.contains("onclick"));
        assertTrue(clean.contains("data:image/png"));
        assertTrue(clean.contains("ok"));
    }

    @Test
    void signatureBlockEscapesContent() {
        String out = HtmlSanitizer.appendSignatureBlock("<html><body><p>minutes</p></body></html>", List.of("Signed by: <b>x</b> & y"));
        assertTrue(out.contains("&lt;b&gt;x&lt;/b&gt; &amp; y"));
        assertTrue(out.contains("ELECTRONIC SIGNATURE"));
    }
}
