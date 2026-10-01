package com.universidad.sigac.service;

import com.universidad.sigac.config.AppProperties;
import com.universidad.sigac.dto.AuthDtos.ForgotPasswordRequest;
import com.universidad.sigac.dto.AuthDtos.LoginRequest;
import com.universidad.sigac.dto.AuthDtos.LoginResponse;
import com.universidad.sigac.dto.AuthDtos.MensajeResponse;
import com.universidad.sigac.dto.AuthDtos.ResetPasswordRequest;
import com.universidad.sigac.entity.PasswordResetToken;
import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.exception.ApiException;
import com.universidad.sigac.repository.PasswordResetTokenRepository;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.JwtService;
import com.universidad.sigac.util.HashUtil;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión (RF-01, CU-01) y recuperación de contraseña (RF-05). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VIGENCIA_TOKEN_MINUTOS = 30;

    private final UsuarioRepository usuarios;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuditService auditoria;
    private final EmailService email;
    private final AppProperties props;

    /** 401 si las credenciales son inválidas; 403 si la cuenta está desactivada. */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        String correo = req.email().trim().toLowerCase();
        Usuario u = usuarios.findByEmailIgnoreCase(correo).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            auditoria.registrarComo("EVENT_LOGIN_FAILED", u == null ? null : u.getId(), correo, "Credenciales inválidas");
            throw ApiException.unauthorized("Credenciales de acceso incorrectas");
        }
        if (!u.isActivo()) {
            auditoria.registrarComo("EVENT_LOGIN_BLOCKED", u.getId(), correo, "Cuenta desactivada");
            throw ApiException.forbidden("Cuenta de usuario desactivada. Contacte a la administración");
        }
        u.setUltimoAcceso(Instant.now());
        String token = jwtService.generar(UsuarioService.aAutenticado(u), props.getJwt().getExpirationHours());
        auditoria.registrarComo("EVENT_LOGIN", u.getId(), correo, "Inicio de sesión exitoso");
        return new LoginResponse(token, "Bearer", jwtService.expiracion(token), UsuarioService.aDto(u));
    }

    /** Responde siempre lo mismo para no revelar si el correo existe. */
    @Transactional
    public MensajeResponse solicitarRecuperacion(ForgotPasswordRequest req) {
        usuarios.findByEmailIgnoreCase(req.email().trim()).filter(Usuario::isActivo).ifPresent(u -> {
            tokens.findByUsuarioIdAndUsadoFalse(u.getId()).forEach(t -> t.setUsado(true));
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            PasswordResetToken t = new PasswordResetToken();
            t.setUsuario(u);
            t.setTokenHash(HashUtil.sha256Hex(token)); // solo se guarda el hash
            t.setExpiraEn(Instant.now().plusSeconds(VIGENCIA_TOKEN_MINUTOS * 60L));
            tokens.save(t);
            auditoria.registrarComo("EVENT_PASSWORD_RESET_REQUESTED", u.getId(), u.getEmail(), null);
            email.enviarRecuperacionClave(u.getEmail(), u.getNombre(), token);
        });
        return new MensajeResponse("Si el correo está registrado, recibirá instrucciones para restablecer su contraseña");
    }

    @Transactional
    public MensajeResponse restablecer(ResetPasswordRequest req) {
        UsuarioService.validarClave(req.nuevaClave());
        PasswordResetToken t = tokens.findByTokenHash(HashUtil.sha256Hex(req.token().trim()))
                .filter(x -> !x.isUsado() && x.getExpiraEn().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.badRequest("El enlace de recuperación no es válido o ya fue utilizado"));
        Usuario u = t.getUsuario();
        u.setPasswordHash(encoder.encode(req.nuevaClave()));
        t.setUsado(true);
        auditoria.registrarComo("EVENT_PASSWORD_RESET", u.getId(), u.getEmail(), null);
        return new MensajeResponse("Contraseña actualizada correctamente");
    }
}
