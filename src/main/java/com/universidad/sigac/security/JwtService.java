package com.universidad.sigac.security;

import com.universidad.sigac.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(AppProperties props) {
        byte[] bytes = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("sigac.jwt.secret debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** Genera el token codificando ID, rol(es) y ámbito jerárquico (RF-01 / CU-01 paso 6). */
    public String generar(UsuarioAutenticado u, long horasExpiracion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(u.email())
                .claim("uid", u.id())
                .claim("nombre", u.nombre())
                .claim("roles", u.roles())
                .claim("fac", u.facultadId())
                .claim("prog", u.programaId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(horasExpiracion * 3600)))
                .signWith(key)
                .compact();
    }

    public Instant expiracion(String token) {
        return parse(token).getExpiration().toInstant();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public UsuarioAutenticado leerUsuario(Claims c) {
        Number uid = c.get("uid", Number.class);
        Number fac = c.get("fac", Number.class);
        Number prog = c.get("prog", Number.class);
        List<String> roles = (List<String>) c.get("roles", List.class);
        return new UsuarioAutenticado(uid.longValue(), c.getSubject(), c.get("nombre", String.class), roles,
                fac == null ? null : fac.longValue(), prog == null ? null : prog.longValue());
    }
}
