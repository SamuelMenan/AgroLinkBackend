package com.agrolink.api;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;

import com.agrolink.api.dto.UserDto;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final String baseUrl;
    private final String anonKey;
    private final RestTemplate rest = new RestTemplate();

    public AuthController() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String url = dotenv.get("SUPABASE_URL");
        String anon = dotenv.get("SUPABASE_ANON_KEY");
        this.baseUrl = url == null ? "" : url.trim();
        this.anonKey = anon == null ? "" : anon.trim();
    }

    private boolean envConfigured() {
        return !baseUrl.isBlank() && !anonKey.isBlank();
    }

    private ResponseEntity<String> missingConfig() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Supabase auth no configurado: define SUPABASE_URL y SUPABASE_ANON_KEY");
    }

    private HttpEntity<Map<String, Object>> buildEntity(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", anonKey);
        headers.set("Authorization", "Bearer " + anonKey);
        return new HttpEntity<>(payload, headers);
    }

    private ResponseEntity<String> forwardPost(String url, Map<String, Object> payload) {
        if (!envConfigured()) return missingConfig();
        try {
            return rest.postForEntity(url, buildEntity(payload), String.class);
        } catch (RestClientResponseException e) {
            // Propagar código y cuerpo exactos de Supabase (400, 422, etc.) en lugar de 500 genérico
            return ResponseEntity.status(e.getRawStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Auth proxy error: " + e.getMessage());
        }
    }

    @PostMapping("/sign-in")
    public ResponseEntity<String> signIn(@RequestBody Map<String, Object> payload) {
        String url = baseUrl + "/auth/v1/token?grant_type=password";
        return forwardPost(url, payload);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<String> signUp(@RequestBody Map<String, Object> payload) {
        String url = baseUrl + "/auth/v1/signup";
        return forwardPost(url, payload);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestBody Map<String, Object> payload) {
        String url = baseUrl + "/auth/v1/token?grant_type=refresh_token";
        return forwardPost(url, payload);
    }

    /**
     * NO IMPLEMENTADO: ahora mismo solo devuelve un mensaje fijo.
     * Endpoint pensado para registrar usuarios en tu propia base de datos.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserDto userDto) {
        // TODO: implementar la lógica real de registro (guardar usuario en BD, validar, etc.)
        return ResponseEntity.ok("Usuario registrado exitosamente (stub: lógica aún no implementada)");
    }

    /**
     * NO IMPLEMENTADO: endpoint que el frontend usa para iniciar OAuth con Google u otros providers.
     * URL esperada:
     *   GET /api/v1/auth/oauth/start?provider=google&next=...
     */
    @GetMapping("/oauth/start")
    public ResponseEntity<String> startOAuth(@RequestParam String provider,
                                             @RequestParam String next) {
        // TODO: implementar redirección real a Supabase OAuth (o al proveedor que uses).
        String msg = "OAuth start endpoint recibido. provider=" + provider + ", next=" + next +
                ". Falta implementar redirección a Supabase.";
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(msg);
    }
}
