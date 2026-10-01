package com.universidad.sigac.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.sigac.exception.ApiError;
import com.universidad.sigac.repository.UsuarioRepository;
import com.universidad.sigac.security.JwtAuthFilter;
import com.universidad.sigac.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Seguridad: API stateless con JWT. Los permisos finos se declaran en los controladores con @PreAuthorize. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** RF-02: BCrypt con factor de costo 10. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UsuarioRepository usuarios,
                                           ObjectMapper mapper) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/v1/auth/**", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> responder(mapper, req, res, HttpStatus.UNAUTHORIZED,
                                "Sesión no válida o expirada. Inicie sesión nuevamente"))
                        .accessDeniedHandler((req, res, ex) -> responder(mapper, req, res, HttpStatus.FORBIDDEN,
                                "No tiene permisos para realizar esta operación")))
                .addFilterBefore(new JwtAuthFilter(jwtService, usuarios), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://localhost:*",
                "https://127.0.0.1:*"));
        for (String origin : props.getCors().getAllowedOrigins()) {
            c.addAllowedOriginPattern(origin);
        }
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Content-Disposition", "X-SIGAC-SHA256"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    private void responder(ObjectMapper mapper, HttpServletRequest req, HttpServletResponse res, HttpStatus status,
                           String mensaje) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), List.of()));
    }
}
