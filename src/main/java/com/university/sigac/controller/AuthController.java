package com.university.sigac.controller;

import com.university.sigac.dto.AuthDtos.ForgotPasswordRequest;
import com.university.sigac.dto.AuthDtos.LoginRequest;
import com.university.sigac.dto.AuthDtos.LoginResponse;
import com.university.sigac.dto.AuthDtos.MessageResponse;
import com.university.sigac.dto.AuthDtos.ResetPasswordRequest;
import com.university.sigac.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints. */
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
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return service.requestRecovery(req);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return service.resetPassword(req);
    }
}
