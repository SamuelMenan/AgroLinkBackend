package com.agrolink.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backend proxy initiator for Supabase OAuth providers.
 * Frontend calls /api/v1/auth/oauth/start?provider=google&next=/simple
 * We generate a state, store mapping, and redirect (302) the browser to Supabase authorize.
 * Callback currently returns tokens in URL fragment (#) which the backend cannot read.
 * Frontend should still parse fragment at /oauth/callback. This removes Supabase URL usage from source code.
 * A full server-side code exchange would require configuring Supabase for PKCE/code flow.
 */
@RestController
@RequestMapping("/api/v1/v1/auth/oauth")
public class OAuthController {

    private static final Map<String, String> STATE_NEXT = new ConcurrentHashMap<>();

    @GetMapping("/start")
    public void start(@RequestParam String provider,
                      @RequestParam(required = false, name = "next") String next,
                      HttpServletResponse response) throws IOException {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String supabaseUrl = trim(dotenv.get("SUPABASE_URL"));
        if (supabaseUrl.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Supabase URL no configurado");
            return;
        }
        if (!isAllowedProvider(provider)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Proveedor no soportado");
            return;
        }
        String state = UUID.randomUUID().toString();
        String nextPath = (next == null || next.isBlank()) ? "/simple" : next;
        STATE_NEXT.put(state, nextPath);
        // We keep frontend callback to process fragment tokens.
        String callback = buildFrontendCallback(nextPath, state);
        String authorizeUrl = supabaseUrl + "/auth/v1/authorize?provider=" +
                url(provider) + "&redirect_to=" + url(callback) + "&state=" + url(state);
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", authorizeUrl);
    }

    @GetMapping("/state-next")
    public ResponseEntity<String> resolve(@RequestParam String state) {
        String next = STATE_NEXT.get(state);
        if (next == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("state desconocido");
        return ResponseEntity.ok(next);
    }

    private static boolean isAllowedProvider(String p) {
        return switch (p.toLowerCase()) {
            case "google", "facebook" -> true;
            default -> false;
        };
    }

    private static String buildFrontendCallback(String next, String state) {
        // Frontend route processes tokens; include next & state for validation if desired.
        String origin = "http://localhost:5174"; // Could be ENV FRONTEND_ORIGIN
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(origin)
                .path("/oauth/callback")
                .queryParam("next", next)
                .queryParam("state", state);
        return b.build().toUriString();
    }

    private static String trim(String v) { return v == null ? "" : v.trim(); }
    private static String url(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
}