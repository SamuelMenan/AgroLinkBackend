package com.agrolink.api;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    private static String sanitizeNext(String n) {
        if (n == null || n.isBlank()) return "/simple";
        if (!n.startsWith("/")) return "/simple";
        if (n.startsWith("//")) return "/simple";
        if (n.startsWith("/oauth/callback")) return "/simple";
        return n;
    }

    @GetMapping("/start")
    public void start(@RequestParam String provider,
                      @RequestParam(required = false, name = "next") String next,
                      @RequestParam(required = false, name = "redirect_to") String redirectTo,
                      HttpServletResponse response) throws IOException {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String supabaseUrl = trim(dotenv.get("SUPABASE_URL"));
        String frontendOriginEnv = trim(dotenv.get("FRONTEND_ORIGIN"));

        String frontendOrigin = frontendOriginEnv.isBlank()
                ? "https://delicate-daifuku-92218d.netlify.app"
                : frontendOriginEnv;

        // Debug logging
        System.out.println("[OAuth Debug] FRONTEND_ORIGIN from env: " + frontendOriginEnv);
        System.out.println("[OAuth Debug] Using frontendOrigin: " + frontendOrigin);
        System.out.println("[OAuth Debug] redirect_to parameter: " + redirectTo);

        if (supabaseUrl.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Supabase URL no configurado");
            return;
        }
        if (!isAllowedProvider(provider)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Proveedor no soportado");
            return;
        }

        String safeNext = sanitizeNext(next);

        // Base de callback: usar redirect_to si viene del cliente; si no, FRONTEND_ORIGIN
        String callbackBase;
        if (redirectTo != null && !redirectTo.isBlank()) {
            callbackBase = redirectTo.trim();
            System.out.println("[OAuth Debug] Using redirect_to parameter: " + callbackBase);
        } else {
            callbackBase = UriComponentsBuilder.fromHttpUrl(frontendOrigin)
                    .path("/oauth/callback")
                    .build()
                    .toUriString();
            System.out.println("[OAuth Debug] Using FRONTEND_ORIGIN: " + callbackBase);
        }

        // Asegurar que enviamos ?next=... al callback del frontend
        String finalRedirectTo = UriComponentsBuilder.fromUriString(Objects.requireNonNull(callbackBase))
                .replaceQuery(null)
                .queryParam("next", safeNext)
                .build()
                .toUriString();

        // Validate that the finalRedirectTo is a proper URL
        try {
            new java.net.URL(finalRedirectTo);
            System.out.println("[OAuth Debug] finalRedirectTo is valid URL: " + finalRedirectTo);
        } catch (Exception e) {
            System.out.println("[OAuth Debug] finalRedirectTo is invalid URL: " + finalRedirectTo + " error: " + e.getMessage());
        }

        // Use UriComponentsBuilder to properly construct the authorize URL
        String authorizeUrl = UriComponentsBuilder.fromHttpUrl(supabaseUrl)
                .path("/auth/v1/authorize")
                .queryParam("provider", provider)
                .queryParam("redirect_to", finalRedirectTo)
                .build()
                .toUriString();

        // Debug logging
        System.out.println("[OAuth Debug] finalRedirectTo: " + finalRedirectTo);
        System.out.println("[OAuth Debug] url(finalRedirectTo): " + url(finalRedirectTo));
        System.out.println("[OAuth Debug] authorizeUrl: " + authorizeUrl);
        System.out.println("[OAuth Debug] provider: " + provider);
        System.out.println("[OAuth Debug] url(provider): " + url(provider));

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
            String frontendOriginEnv = trim(dotenv.get("FRONTEND_ORIGIN"));
            String frontendOrigin = frontendOriginEnv.isBlank()
                    ? "https://delicate-daifuku-92218d.netlify.app"
                    : frontendOriginEnv;

            redirectUri = UriComponentsBuilder.fromHttpUrl(frontendOrigin)
                    .path("/oauth/callback")
                    .build()
                    .toUriString();
        }

        try {
            SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
            rf.setConnectTimeout(5000);
            rf.setReadTimeout(10000);
            RestTemplate restTemplate = new RestTemplate(rf);
            URI tokenUri = URI.create(Objects.requireNonNull(supabaseUrl) + "/auth/v1/token?grant_type=authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", serviceKey);
            headers.set("Authorization", "Bearer " + serviceKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("code", code);
            payload.put("redirect_uri", redirectUri);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
                @SuppressWarnings({"unchecked", "null"})
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

    private static boolean isAllowedProvider(String p) {
        return switch (p.toLowerCase()) {
            case "google", "facebook" -> true;
            default -> false;
        };
    }

    private static String trim(String v) { return v == null ? "" : v.trim(); }
    private static String url(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
}
