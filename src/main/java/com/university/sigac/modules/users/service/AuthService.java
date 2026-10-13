package com.university.sigac.modules.users.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.security.JwtService;
import com.university.sigac.core.util.HashUtil;
import com.university.sigac.modules.notifications.service.EmailService;
import com.university.sigac.modules.users.dto.AuthDtos.ForgotPasswordRequest;
import com.university.sigac.modules.users.dto.AuthDtos.LoginRequest;
import com.university.sigac.modules.users.dto.AuthDtos.LoginResponse;
import com.university.sigac.modules.users.dto.AuthDtos.MessageResponse;
import com.university.sigac.modules.users.dto.AuthDtos.ResetPasswordRequest;
import com.university.sigac.modules.users.entity.PasswordResetToken;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.repository.PasswordResetTokenRepository;
import com.university.sigac.modules.users.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sign-in (RF-01, UC-01) and password recovery (RF-05). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuditService audit;
    private final EmailService email;
    private final AppProperties props;

    /** 401 if the credentials are invalid; 403 if the account is deactivated. */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        String address = req.email().trim().toLowerCase();
        User u = users.findByEmailIgnoreCase(address).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            audit.recordAs("EVENT_LOGIN_FAILED", u == null ? null : u.getId(), address, "Invalid credentials");
            throw ApiException.unauthorized("Incorrect sign-in credentials");
        }
        if (!u.isActive()) {
            audit.recordAs("EVENT_LOGIN_BLOCKED", u.getId(), address, "Account deactivated");
            throw ApiException.forbidden("User account deactivated. Please contact the administration");
        }
        u.setLastLogin(Instant.now());
        String token = jwtService.generate(UserService.toAuthenticated(u), props.getJwt().getExpirationHours());
        audit.recordAs("EVENT_LOGIN", u.getId(), address, "Successful sign-in");
        return new LoginResponse(token, "Bearer", jwtService.expiration(token), UserService.toDto(u));
    }

    /** Always returns the same answer so as not to reveal whether the email exists. */
    @Transactional
    public MessageResponse requestRecovery(ForgotPasswordRequest req) {
        users.findByEmailIgnoreCase(req.email().trim()).filter(User::isActive).ifPresent(u -> {
            tokens.findByUserIdAndUsedFalse(u.getId()).forEach(t -> t.setUsed(true));
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            PasswordResetToken t = new PasswordResetToken();
            t.setUser(u);
            t.setTokenHash(HashUtil.sha256Hex(token)); // only the hash is stored
            t.setExpiresAt(Instant.now().plusSeconds(TOKEN_VALIDITY_MINUTES * 60L));
            tokens.save(t);
            audit.recordAs("EVENT_PASSWORD_RESET_REQUESTED", u.getId(), u.getEmail(), null);
            email.sendPasswordReset(u.getEmail(), u.getName(), token);
        });
        return new MessageResponse("If the email is registered, you will receive instructions to reset your password");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        UserService.validatePassword(req.newPassword());
        PasswordResetToken t = tokens.findByTokenHash(HashUtil.sha256Hex(req.token().trim()))
                .filter(x -> !x.isUsed() && x.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.badRequest("The recovery link is invalid or has already been used"));
        User u = t.getUser();
        u.setPasswordHash(encoder.encode(req.newPassword()));
        t.setUsed(true);
        audit.recordAs("EVENT_PASSWORD_RESET", u.getId(), u.getEmail(), null);
        return new MessageResponse("Password updated successfully");
    }
}
