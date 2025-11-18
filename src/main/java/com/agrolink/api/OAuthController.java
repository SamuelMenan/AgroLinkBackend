package com.agrolink.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    @GetMapping("/start")
    public void start(@RequestParam String provider,
                      @RequestParam(required = false, name = "next") String next,
                      HttpServletResponse response) throws IOException {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String supabaseUrl = trim(dotenv.get("SUPABASE_URL"));
        String frontendOriginEnv = trim(dotenv.get("FRONTEND_ORIGIN"));

        // Si no se define FRONTEND_ORIGIN, en producción debes configurarla en Render.
        // Como fallback mantenemos localhost solo para entorno de desarrollo.
        String frontendOrigin = frontendOriginEnv.isBlank()
                ? "http://localhost:5174"
                : frontendOriginEnv;

        if (supabaseUrl.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Supabase URL no configurado");
            return;
        }
        if (!isAllowedProvider(provider)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Proveedor no soportado");
            return;
        }

        String nextPath = (next == null || next.isBlank()) ? "/simple" : next;
        // Construye callback tipo: https://frontend/origin/oauth/callback?next=...
        String callback = buildFrontendCallback(frontendOrigin, nextPath);

        String authorizeUrl = supabaseUrl + "/auth/v1/authorize?provider=" +
                url(provider) + "&redirect_to=" + url(callback);

        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", authorizeUrl);
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> body) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String supabaseUrl = trim(dotenv.get("SUPABASE_URL"));
        String serviceKey = trim(dotenv.get("SUPABASE_SERVICE_KEY"));
        if (supabaseUrl.isBlank() || serviceKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Supabase no configurado para intercambio de código"));
        }

        String code = trim(body.get("code"));
        if (code.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "code requerido"));
        }

        String redirectUri = trim(body.get("redirectUri"));
        if (redirectUri.isBlank()) {
            // Fallback: usar FRONTEND_ORIGIN o localhost solo en dev
            String frontendOriginEnv = trim(dotenv.get("FRONTEND_ORIGIN"));
            String frontendOrigin = frontendOriginEnv.isBlank()
                    ? "http://localhost:5174"
                    : frontendOriginEnv;

            redirectUri = UriComponentsBuilder.fromHttpUrl(frontendOrigin)
                    .path("/oauth/callback")
                    .build()
                    .toUriString();
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            URI tokenUri = URI.create(supabaseUrl + "/auth/v1/token?grant_type=authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", serviceKey);
            headers.set("Authorization", "Bearer " + serviceKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("code", code);
            payload.put("redirect_uri", redirectUri);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> resp = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>)
                    restTemplate.exchange(tokenUri, HttpMethod.POST, requestEntity, Map.class);

            Map<String, Object> bodyMap = resp.getBody();
            if (!resp.getStatusCode().is2xxSuccessful() || bodyMap == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Fallo al intercambiar código en Supabase"));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("access_token", bodyMap.get("access_token"));
            result.put("refresh_token", bodyMap.get("refresh_token"));
            result.put("expires_in", bodyMap.get("expires_in"));
            result.put("token_type", bodyMap.get("token_type"));
            if (bodyMap.containsKey("user")) {
                result.put("user", bodyMap.get("user"));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Excepción intercambiando código: " + e.getMessage()));
        }
    }

    // Deprecated: state resolution no longer needed; Supabase implicit flow does not echo custom state reliably.
    @GetMapping("/state-next")
    public ResponseEntity<String> resolve() {
        return ResponseEntity.status(HttpStatus.GONE).body("state mapping removido");
    }

    private static boolean isAllowedProvider(String p) {
        return switch (p.toLowerCase()) {
            case "google", "facebook" -> true;
            default -> false;
        };
    }

    private static String buildFrontendCallback(String origin, String next) {
        return UriComponentsBuilder.fromHttpUrl(origin)
                .path("/oauth/callback")
                .queryParam("next", next)
                .build()
                .toUriString();
    }

    private static String trim(String v) { return v == null ? "" : v.trim(); }
    private static String url(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }

    // Método auxiliar de desarrollo; no se usa en producción.
    public static String getOAuthStartUrl(String provider, String next) {
        return "http://localhost:8080/api/v1/auth/oauth/start?provider=" +
                URLEncoder.encode(provider, StandardCharsets.UTF_8) +
                "&next=" + URLEncoder.encode(next, StandardCharsets.UTF_8);
    }
}