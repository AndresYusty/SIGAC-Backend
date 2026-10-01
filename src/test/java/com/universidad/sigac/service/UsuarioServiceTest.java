package com.universidad.sigac.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.universidad.sigac.exception.ApiException;
import org.junit.jupiter.api.Test;

class UsuarioServiceTest {

    @Test
    void claveValidaTieneLetrasNumerosYMinimoOchoCaracteres() {
        assertDoesNotThrow(() -> UsuarioService.validarClave("Segura2026"));
    }

    @Test
    void rechazaClavesDebiles() {
        assertThrows(ApiException.class, () -> UsuarioService.validarClave("corta1"));
        assertThrows(ApiException.class, () -> UsuarioService.validarClave("sinnumeros"));
        assertThrows(ApiException.class, () -> UsuarioService.validarClave("12345678"));
        assertThrows(ApiException.class, () -> UsuarioService.validarClave(null));
    }
}
