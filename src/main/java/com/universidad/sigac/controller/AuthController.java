package com.universidad.sigac.controller;

import com.universidad.sigac.dto.AuthDtos.ForgotPasswordRequest;
import com.universidad.sigac.dto.AuthDtos.LoginRequest;
import com.universidad.sigac.dto.AuthDtos.LoginResponse;
import com.universidad.sigac.dto.AuthDtos.MensajeResponse;
import com.universidad.sigac.dto.AuthDtos.ResetPasswordRequest;
import com.universidad.sigac.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos de autenticación. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @PostMapping("/forgot-password")
    public MensajeResponse olvideMiClave(@Valid @RequestBody ForgotPasswordRequest req) {
        return service.solicitarRecuperacion(req);
    }

    @PostMapping("/reset-password")
    public MensajeResponse restablecer(@Valid @RequestBody ResetPasswordRequest req) {
        return service.restablecer(req);
    }
}
