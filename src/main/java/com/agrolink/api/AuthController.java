package com.agrolink.api;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;

import com.agrolink.api.dto.UserDto;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final String baseUrl;
    private final String anonKey;
    private final RestTemplate rest;

    public AuthController() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String url = resolveEnv(dotenv, "SUPABASE_URL");
        String anon = resolveEnv(dotenv, "SUPABASE_ANON_KEY");
        this.baseUrl = url == null ? "" : url.trim();
        this.anonKey = anon == null ? "" : anon.trim();
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(10000);
        this.rest = new RestTemplate(rf);
    }

    private boolean envConfigured() {
        return !baseUrl.isBlank() && !anonKey.isBlank();
    }

    private String resolveEnv(Dotenv dotenv, String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String dv = dotenv.get(key);
        return (dv == null || dv.isBlank()) ? null : dv.trim();
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

    @SuppressWarnings("null")
    private ResponseEntity<String> forwardPost(String url, Map<String, Object> payload) {
        if (!envConfigured()) return missingConfig();
        try {
            long start = System.currentTimeMillis();
            ResponseEntity<String> resp = rest.postForEntity(url, buildEntity(payload), String.class);
            long took = System.currentTimeMillis() - start;
            String path = url.replaceFirst("https?://[^/]+", "");
            // No loggear tokens; sólo meta-información
            System.out.println("[AuthController] POST " + path + " -> status=" + resp.getStatusCode().value() + " in " + took + "ms");
            return resp;
        } catch (RestClientResponseException e) {
            String path = url.replaceFirst("https?://[^/]+", "");
            System.err.println("[AuthController] POST " + path + " error status=" + e.getStatusCode().value() + " bodyLen=" + (e.getResponseBodyAsString()!=null? e.getResponseBodyAsString().length():0));
            // Propagar código y cuerpo exactos de Supabase (400, 422, etc.) en lugar de 500 genérico
            return ResponseEntity.status(e.getStatusCode().value())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            String path = url.replaceFirst("https?://[^/]+", "");
            System.err.println("[AuthController] POST " + path + " exception: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
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

    // El endpoint de OAuth (/api/v1/auth/oauth/start) está definido en OAuthController#start(...)
    // para evitar duplicar mappings aquí.
}
