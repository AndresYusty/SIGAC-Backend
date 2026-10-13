package com.university.sigac.core.security;

import com.university.sigac.config.AppProperties;
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
            throw new IllegalStateException("sigac.jwt.secret must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** Generates the token encoding ID, role(s) and hierarchical scope (RF-01 / UC-01 step 6). */
    public String generate(AuthenticatedUser u, long expirationHours) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(u.email())
                .claim("uid", u.id())
                .claim("name", u.name())
                .claim("roles", u.roles())
                .claim("fac", u.facultyId())
                .claim("prog", u.programId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationHours * 3600)))
                .signWith(key)
                .compact();
    }

    public Instant expiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser readUser(Claims c) {
        Number uid = c.get("uid", Number.class);
        Number fac = c.get("fac", Number.class);
        Number prog = c.get("prog", Number.class);
        List<String> roles = (List<String>) c.get("roles", List.class);
        return new AuthenticatedUser(uid.longValue(), c.getSubject(), c.get("name", String.class), roles,
                fac == null ? null : fac.longValue(), prog == null ? null : prog.longValue());
    }
}
