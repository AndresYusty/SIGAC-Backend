package com.university.sigac.modules.users.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.university.sigac.core.exception.ApiException;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    @Test
    void validPasswordHasLettersDigitsAndAtLeastEightCharacters() {
        assertDoesNotThrow(() -> UserService.validatePassword("Secure2026"));
    }

    @Test
    void rejectsWeakPasswords() {
        assertThrows(ApiException.class, () -> UserService.validatePassword("short1"));
        assertThrows(ApiException.class, () -> UserService.validatePassword("nodigits"));
        assertThrows(ApiException.class, () -> UserService.validatePassword("12345678"));
        assertThrows(ApiException.class, () -> UserService.validatePassword(null));
    }
}
