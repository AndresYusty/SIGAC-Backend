package com.universidad.sigac.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, String tipo, Instant expiraEn, UsuarioDtos.UsuarioDto usuario) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 100) String nuevaClave) {
    }

    public record MensajeResponse(String mensaje) {
    }
}
