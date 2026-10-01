package com.universidad.sigac.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.sigac.util.HtmlSanitizer;
import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    @Test
    void notasConservanFormatoBasicoYEliminanScripts() {
        String limpio = HtmlSanitizer.limpiarFragmento("<p>Hola <b>mundo</b></p><script>alert(1)</script><img src=\"http://x/y.png\">");
        assertTrue(limpio.contains("<b>mundo</b>"));
        assertFalse(limpio.contains("script"));
        assertFalse(limpio.contains("img"));
    }

    @Test
    void documentoEliminaRecursosExternosPeroConservaDataUri() {
        String html = "<html><body><img src=\"http://evil/x.png\"/><img src=\"data:image/png;base64,AAAA\"/>"
                + "<p onclick=\"x()\" style=\"background:url(http://evil/a.png)\">ok</p></body></html>";
        String limpio = HtmlSanitizer.sanitizarDocumento(html);
        assertFalse(limpio.contains("evil"));
        assertFalse(limpio.contains("onclick"));
        assertTrue(limpio.contains("data:image/png"));
        assertTrue(limpio.contains("ok"));
    }

    @Test
    void bloqueFirmaEscapaElContenido() {
        String out = HtmlSanitizer.agregarBloqueFirma("<html><body><p>acta</p></body></html>", List.of("Firmado por: <b>x</b> & y"));
        assertTrue(out.contains("&lt;b&gt;x&lt;/b&gt; &amp; y"));
        assertTrue(out.contains("FIRMA ELECTR"));
    }
}
