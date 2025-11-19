package com.agrolink.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String reqId = java.util.UUID.randomUUID().toString().substring(0,8);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String ua = request.getHeader("User-Agent");
        String contentType = request.getContentType();
        String auth = request.getHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            // Sanitizar el token
            auth = auth.replaceAll("(?i)Bearer\\s+([A-Za-z0-9-_]+)\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+", "Bearer ***.***.***");
        }
        log.info("[HTTP:{}] {} {}{} origin={} referer={} ua={} contentType={} auth={} ", reqId, method, uri, (query!=null? ("?"+query):""), origin, referer, ua, contentType, auth);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long took = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String aco = response.getHeader("Access-Control-Allow-Origin");
            String acc = response.getHeader("Access-Control-Allow-Credentials");
            String vary = response.getHeader("Vary");
            String expose = response.getHeader("Access-Control-Expose-Headers");
            String msg = String.format("[HTTP:%s] %s %s%s -> status=%d in %dms CORS[origin=%s,creds=%s,expose=%s,vary=%s]", reqId, method, uri, (query!=null? ("?"+query):""), status, took, aco, acc, expose, vary);
            if (status >= 500) log.error(msg); else log.info(msg);
        }
    }
}
