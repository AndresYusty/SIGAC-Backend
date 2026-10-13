package com.university.sigac.modules.templates.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateValidatorTest {

    private final TemplateValidator validator = new TemplateValidator();

    private String defaultTemplate() throws Exception {
        try (var in = getClass().getResourceAsStream("/sigac/templates/minutes-default.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String withBody(String body) {
        return "<html xmlns:th=\"http://www.thymeleaf.org\"><body><p th:text=\"${serialNumber}\">x</p>"
                + "<div th:each=\"a : ${agreements}\">" + body + "</div></body></html>";
    }

    @Test
    void defaultTemplateIsValid() throws Exception {
        List<String> errors = validator.validate(defaultTemplate());
        assertTrue(errors.isEmpty(), () -> "Errors: " + errors);
    }

    @Test
    void detectsUnclosedTags() {
        assertFalse(validator.validate("<html><body><p>not closed</body></html>").isEmpty());
    }

    @Test
    void rejectsVariablesOutsideTheCatalog() {
        List<String> e = validator.validate(withBody("<span th:text=\"${secret}\">x</span>"));
        assertTrue(e.stream().anyMatch(s -> s.contains("secret")));
    }

    @Test
    void rejectsExpressionInjection() {
        List<String> e = validator.validate(withBody("<span th:text=\"${T(java.lang.Runtime).getRuntime()}\">x</span>"));
        assertFalse(e.isEmpty());
        List<String> e2 = validator.validate(withBody("<span th:text=\"${a.class.classLoader}\">x</span>"));
        assertFalse(e2.isEmpty());
    }

    @Test
    void rejectsDisallowedThAttributes() {
        List<String> e = validator.validate(withBody("<div th:replace=\"~{other}\">x</div>"));
        assertFalse(e.isEmpty());
        List<String> e2 = validator.validate(withBody("<img th:src=\"'http://evil'\"/>"));
        assertFalse(e2.isEmpty());
    }

    @Test
    void rejectsExternalResources() {
        List<String> e = validator.validate(withBody("<img src=\"http://evil.example/x.png\"/>"));
        assertFalse(e.isEmpty());
        List<String> e2 = validator.validate(withBody("<script>alert(1)</script>"));
        assertFalse(e2.isEmpty());
    }

    @Test
    void requiresSerialNumberAndAgreements() {
        List<String> e = validator.validate("<html><body><p>hello</p></body></html>");
        assertTrue(e.stream().anyMatch(s -> s.contains("serialNumber")));
        assertTrue(e.stream().anyMatch(s -> s.contains("agreements")));
    }
}
