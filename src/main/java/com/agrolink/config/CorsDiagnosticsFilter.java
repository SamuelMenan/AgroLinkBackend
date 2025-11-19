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
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.util.Optional;

/**
 * Diagnóstico adicional: detecta casos donde Origin presente pero falta ACAO.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CorsDiagnosticsFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(CorsDiagnosticsFilter.class);

    private static final String frontProd;
    private static final String frontDev;
    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        frontProd = resolveEnv(dotenv, "FRONTEND_ORIGIN").orElse("https://agro-link-jet.vercel.app");
        frontDev = resolveEnv(dotenv, "FRONTEND_ORIGIN_DEV").orElse("http://localhost:5173");
    }

    private static Optional<String> resolveEnv(Dotenv dotenv, String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return Optional.of(sys.trim());
        String dv = dotenv.get(key);
        if (dv != null && !dv.isBlank()) return Optional.of(dv.trim());
        return Optional.empty();
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        // Exact matches
        if (origin.equals(frontProd) || origin.equals(frontDev)) return true;
        // Wildcard *.vercel.app
        if (origin.startsWith("https://") && origin.endsWith(".vercel.app")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        String origin = request.getHeader("Origin");
        String acao = response.getHeader("Access-Control-Allow-Origin");
        if (origin != null && acao == null) {
            if (isAllowedOrigin(origin)) {
                // Auto parcheo de cabeceras CORS faltantes
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
                response.addHeader("Vary", "Origin");
                log.warn("[CORS-DIAG] Missing ACAO patched. method={} path={} origin={}", request.getMethod(), request.getRequestURI(), origin);
            } else {
                log.warn("[CORS-DIAG] Origin present but no ACAO header (not allowed). method={} path={} origin={}", request.getMethod(), request.getRequestURI(), origin);
            }
        }
    }
}
