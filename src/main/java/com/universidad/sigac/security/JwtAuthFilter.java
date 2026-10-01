package com.universidad.sigac.security;

import com.universidad.sigac.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro stateless (RN-03): lee el token "Authorization: Bearer ...", lo valida y deja al usuario en el
 * SecurityContext. También comprueba que la cuenta siga activa.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarios;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarios) {
        this.jwtService = jwtService;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UsuarioAutenticado user = jwtService.leerUsuario(jwtService.parse(header.substring(7).trim()));
                if (usuarios.existsByIdAndActivoTrue(user.id())) {
                    List<GrantedAuthority> permisos = user.roles().stream()
                            .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList();
                    SecurityContextHolder.getContext()
                            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, permisos));
                }
            } catch (JwtException | IllegalArgumentException | ClassCastException | NullPointerException e) {
                SecurityContextHolder.clearContext(); // token inválido: la petición seguirá sin autenticar (401)
            }
        }
        chain.doFilter(request, response);
    }
}
