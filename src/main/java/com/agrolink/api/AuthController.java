package com.agrolink.api;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.core.ParameterizedTypeReference;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agrolink.api.dto.UserDto;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final String baseUrl;
    private final String anonKey;
    private final RestTemplate rest;
    private final String hcaptchaSecret;
    private final String recaptchaSecret;
    private final String serviceKey;

    public AuthController() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String url = resolveEnv(dotenv, "SUPABASE_URL");
        String anon = resolveEnv(dotenv, "SUPABASE_ANON_KEY");
        this.baseUrl = url == null ? "" : url.trim();
        this.anonKey = anon == null ? "" : anon.trim();
        String hc = resolveEnv(dotenv, "HCAPTCHA_SECRET");
        this.hcaptchaSecret = hc == null ? "" : hc.trim();
        String rc = resolveEnv(dotenv, "RECAPTCHA_SECRET");
        this.recaptchaSecret = rc == null ? "" : rc.trim();
        String sk = resolveEnv(dotenv, "SUPABASE_SERVICE_KEY");
        this.serviceKey = sk == null ? "" : sk.trim();
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

    private HttpEntity<Map<String, Object>> buildEntityNoVerify(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", anonKey);
        headers.set("Authorization", "Bearer " + anonKey);
        // Disable email verification for direct registration
        payload.put("email_confirm", true);
        payload.put("skip_confirmation", true);
        return new HttpEntity<>(payload, headers);
    }

    private HttpEntity<Map<String, Object>> buildServiceEntity(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", serviceKey);
        headers.set("Authorization", "Bearer " + serviceKey);
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
            String errorId = UUID.randomUUID().toString();
            Map<String, Object> safe = new HashMap<>(payload == null ? Map.of() : payload);
            safe.remove("password");
            safe.remove("recaptcha_token");
            safe.remove("hcaptcha_token");
            safe.remove("captcha_token");
            log.error("[{}] POST {} unexpected_failure: {}", errorId, path, e.getMessage(), e);
            String body = String.format("{\"code\":503,\"error_code\":\"unexpected_failure\",\"message\":\"Fallo inesperado al procesar la solicitud\",\"error_id\":\"%s\"}", errorId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    @PostMapping("/sign-in")
    public ResponseEntity<String> signIn(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String url = baseUrl + "/auth/v1/token?grant_type=password";
        Object tokenObj = payload.get("recaptcha_token");
        if (tokenObj == null) tokenObj = payload.get("hcaptcha_token");
        if (tokenObj == null) tokenObj = payload.get("captcha_token");
        if (tokenObj != null) {
            String token = tokenObj.toString().trim();
            if (!token.isEmpty()) payload.put("captcha_token", token);
        }
        payload.remove("recaptcha_token");
        payload.remove("hcaptcha_token");
        return forwardPost(url, payload);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<String> signUp(@RequestBody Map<String, Object> payload) {
        String url = baseUrl + "/auth/v1/signup";
        System.out.println("[AuthController] Incoming sign-up request envConfigured=" + envConfigured() + " baseUrl=" + baseUrl + " anonKey.len=" + (anonKey!=null?anonKey.length():0));

        // Bypass temporal de captcha en registro: eliminar cualquier token si viniera del cliente
        if (payload != null) {
            payload.remove("recaptcha_token");
            payload.remove("hcaptcha_token");
            payload.remove("captcha_token");
        }

        // Support phone-only registration - generate email from phone if needed
        String email = (String) payload.get("email");
        String phone = (String) payload.get("phone");
        
        if ((email == null || email.trim().isEmpty()) && (phone != null && !phone.trim().isEmpty())) {
            // Generate email from phone for phone-only registration
            String generatedEmail = phone.trim() + "@phone.user";
            payload.put("email", generatedEmail);
            System.out.println("[AuthController] Phone-only registration: generated email " + generatedEmail + " for phone " + phone);
            email = generatedEmail; // Update email variable for existence check
        }
        
        // Disable email verification - direct registration
        payload.put("email_confirm", true);
        payload.put("skip_confirmation", true);
        
        if (!envConfigured()) return missingConfig();
        
        // Pre-registration existence check
        if (email != null && !email.trim().isEmpty()) {
            try {
                System.out.println("[AuthController] Checking if user exists before registration for email: " + email);
                
                // Try to sign in to check if user exists
                String checkUrl = baseUrl + "/auth/v1/token?grant_type=password";
                Map<String, Object> checkPayload = Map.of(
                    "email", email,
                    "password", payload.get("password") // Use the same password for check
                );
                
                ResponseEntity<String> checkResp = rest.postForEntity(checkUrl, buildEntity(checkPayload), String.class);
                int checkStatus = checkResp.getStatusCode().value();
                // If we get here, sign-in succeeded - user exists (200 range)
                System.err.println("[AuthController] User already exists for email: " + email + " (sign-in succeeded status=" + checkStatus + ")");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("{\"code\":422,\"error_code\":\"user_already_exists\",\"msg\":\"User already registered\"}");
                        
            } catch (RestClientResponseException checkEx) {
                if (checkEx.getStatusCode().value() == 400 || checkEx.getStatusCode().value() == 401) {
                    // Sign-in failed - user doesn't exist or wrong password, proceed with registration
                    System.out.println("[AuthController] User does not exist or invalid credentials for email: " + email + " (sign-in failed with " + checkEx.getStatusCode().value() + ")");
                } else {
                    // Other error during check, log but proceed with registration
                    System.err.println("[AuthController] Unexpected error during user existence check for email: " + email + " status=" + checkEx.getStatusCode().value());
                }
            } catch (Exception checkEx) {
                // Network or other error during check, log but proceed with registration
                System.err.println("[AuthController] Exception during user existence check: " + checkEx.getClass().getSimpleName() + " -> " + checkEx.getMessage());
            }
        }
        
        try {
            long start = System.currentTimeMillis();

            if (!serviceKey.isBlank()) {
                Map<String, Object> adminPayload = new HashMap<>();
                if (email != null && !email.trim().isEmpty()) adminPayload.put("email", email.trim());
                if (phone != null && !phone.trim().isEmpty()) adminPayload.put("phone", phone.trim());
                adminPayload.put("password", Objects.requireNonNull(payload.get("password")).toString());
                Object meta = payload.get("data");
                if (meta instanceof Map<?, ?>) adminPayload.put("user_metadata", meta);
                else adminPayload.put("user_metadata", Map.of());
                adminPayload.put("email_confirm", true);
                HttpEntity<Map<String, Object>> svcEntity = buildServiceEntity(adminPayload);
                ResponseEntity<String> createResp = rest.postForEntity(baseUrl + "/auth/v1/admin/users", svcEntity, String.class);

                Map<String, Object> signPayload = new HashMap<>();
                signPayload.put("password", Objects.requireNonNull(payload.get("password")).toString());
                if (email != null && !email.trim().isEmpty()) signPayload.put("email", email.trim());
                else if (phone != null && !phone.trim().isEmpty()) signPayload.put("phone", phone.trim());
                ResponseEntity<String> resp = rest.postForEntity(baseUrl + "/auth/v1/token?grant_type=password", buildEntity(signPayload), String.class);
                long took = System.currentTimeMillis() - start;
                String path = url.replaceFirst("https?://[^/]+", "");
                System.out.println("[AuthController] Admin registration + sign-in POST " + path + " -> status=" + resp.getStatusCode().value() + " in " + took + "ms bodyLen=" + (resp.getBody()!=null?resp.getBody().length():0));
                return resp;
            }

            HttpEntity<Map<String, Object>> entity = buildEntityNoVerify(payload);
            ResponseEntity<String> resp = rest.postForEntity(url, entity, String.class);
            
            long took = System.currentTimeMillis() - start;
            String path = url.replaceFirst("https?://[^/]+", "");
            
            // Log successful registration without verification
            String body = resp.getBody();
            int bodyLen = body == null ? 0 : body.length();
            System.out.println("[AuthController] Direct registration POST " + path + " -> status=" + resp.getStatusCode().value() + " in " + took + "ms (email verification disabled) bodyLen=" + bodyLen);
            
            return resp;
        } catch (RestClientResponseException e) {
            String path = url.replaceFirst("https?://[^/]+", "");
            String body = e.getResponseBodyAsString();
            String errorId = UUID.randomUUID().toString();
            log.warn("[{}] sign-up upstream_error {}: status={} bodyLen={}", errorId, path, e.getStatusCode().value(), (body != null ? body.length() : 0));

            // Mantener el status original de Supabase pero agregar un envoltorio con error_id
            String safeMessage = "Fallo al registrar usuario";
            String wrapped = String.format("{\"code\":%d,\"error_code\":\"upstream_error\",\"message\":\"%s\",\"error_id\":\"%s\",\"detail\":%s}",
                    e.getRawStatusCode(), safeMessage, errorId, (body != null && body.trim().startsWith("{") ? body : ('\"' + body + '\"')));
            return ResponseEntity.status(e.getStatusCode().value()).body(wrapped);
        } catch (Exception e) {
            String path = url.replaceFirst("https?://[^/]+", "");
            String errorId = UUID.randomUUID().toString();
            Map<String, Object> safe = new HashMap<>(payload == null ? Map.of() : payload);
            safe.remove("password");
            safe.remove("recaptcha_token");
            safe.remove("hcaptcha_token");
            safe.remove("captcha_token");
            log.error("[{}] sign-up unexpected_failure on {}: {}", errorId, path, e.getMessage(), e);
            String body = String.format("{\"code\":503,\"error_code\":\"unexpected_failure\",\"message\":\"No se pudo completar el registro. Por favor, inténtalo más tarde\",\"error_id\":\"%s\"}", errorId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
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
        return ResponseEntity.ok("Usuario registrado exitosamente (stub: lógica aún no implementada)");
    }

    // El endpoint de OAuth (/api/v1/auth/oauth/start) está definido en OAuthController#start(...)
    // para evitar duplicar mappings aquí.
}
