package com.universidad.sigac.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlantillaValidatorTest {

    private final PlantillaValidator validator = new PlantillaValidator();

    private String plantillaPorDefecto() throws Exception {
        try (var in = getClass().getResourceAsStream("/sigac/plantillas/acta-default.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String conCuerpo(String cuerpo) {
        return "<html xmlns:th=\"http://www.thymeleaf.org\"><body><p th:text=\"${consecutivo}\">x</p>"
                + "<div th:each=\"a : ${acuerdos}\">" + cuerpo + "</div></body></html>";
    }

    @Test
    void plantillaPorDefectoEsValida() throws Exception {
        List<String> errores = validator.validar(plantillaPorDefecto());
        assertTrue(errores.isEmpty(), () -> "Errores: " + errores);
    }

    @Test
    void detectaEtiquetasMalCerradas() {
        assertFalse(validator.validar("<html><body><p>sin cerrar</body></html>").isEmpty());
    }

    @Test
    void rechazaVariablesFueraDelCatalogo() {
        List<String> e = validator.validar(conCuerpo("<span th:text=\"${secreto}\">x</span>"));
        assertTrue(e.stream().anyMatch(s -> s.contains("secreto")));
    }

    @Test
    void rechazaInyeccionDeExpresiones() {
        List<String> e = validator.validar(conCuerpo("<span th:text=\"${T(java.lang.Runtime).getRuntime()}\">x</span>"));
        assertFalse(e.isEmpty());
        List<String> e2 = validator.validar(conCuerpo("<span th:text=\"${a.class.classLoader}\">x</span>"));
        assertFalse(e2.isEmpty());
    }

    @Test
    void rechazaAtributosThNoPermitidos() {
        List<String> e = validator.validar(conCuerpo("<div th:replace=\"~{otra}\">x</div>"));
        assertFalse(e.isEmpty());
        List<String> e2 = validator.validar(conCuerpo("<img th:src=\"'http://evil'\"/>"));
        assertFalse(e2.isEmpty());
    }

    @Test
    void rechazaRecursosExternos() {
        List<String> e = validator.validar(conCuerpo("<img src=\"http://evil.example/x.png\"/>"));
        assertFalse(e.isEmpty());
        List<String> e2 = validator.validar(conCuerpo("<script>alert(1)</script>"));
        assertFalse(e2.isEmpty());
    }

    @Test
    void exigeConsecutivoYAcuerdos() {
        List<String> e = validator.validar("<html><body><p>hola</p></body></html>");
        assertTrue(e.stream().anyMatch(s -> s.contains("consecutivo")));
        assertTrue(e.stream().anyMatch(s -> s.contains("acuerdos")));
    }
}
