package com.university.sigac.core.security;

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
 * Stateless filter (RN-03): reads the "Authorization: Bearer ..." token, validates it and places the user in the
 * SecurityContext. It also checks that the account is still active.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ActiveUserChecker activeUsers;

    public JwtAuthFilter(JwtService jwtService, ActiveUserChecker activeUsers) {
        this.jwtService = jwtService;
        this.activeUsers = activeUsers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthenticatedUser user = jwtService.readUser(jwtService.parse(header.substring(7).trim()));
                if (activeUsers.isActive(user.id())) {
                    List<GrantedAuthority> authorities = user.roles().stream()
                            .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList();
                    SecurityContextHolder.getContext()
                            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
                }
            } catch (JwtException | IllegalArgumentException | ClassCastException | NullPointerException e) {
                SecurityContextHolder.clearContext(); // invalid token: the request continues unauthenticated (401)
            }
        }
        chain.doFilter(request, response);
    }
}
