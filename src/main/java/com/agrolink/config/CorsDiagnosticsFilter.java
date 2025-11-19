package com.agrolink.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Diagnóstico adicional: detecta casos donde Origin presente pero falta ACAO.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CorsDiagnosticsFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(CorsDiagnosticsFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        String origin = request.getHeader("Origin");
        String acao = response.getHeader("Access-Control-Allow-Origin");
        if (origin != null && acao == null) {
            log.warn("[CORS-DIAG] Origin present but no ACAO header. method={} path={} origin={}", request.getMethod(), request.getRequestURI(), origin);
        }
    }
}
