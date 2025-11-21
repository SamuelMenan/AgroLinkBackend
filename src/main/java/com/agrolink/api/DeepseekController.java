package com.agrolink.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

@RestController
@RequestMapping("/api/v1/ai/deepseek")
public class DeepseekController {
    private final RestTemplate rest;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public DeepseekController() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String b = resolveEnv(dotenv, "OPENROUTER_BASE_URL");
        String k = resolveEnv(dotenv, "OPENROUTER_API_KEY");
        String m = resolveEnv(dotenv, "OPENROUTER_MODEL");
        this.baseUrl = (b == null || b.isBlank()) ? "https://openrouter.ai/api/v1" : b.trim();
        this.apiKey = (k == null) ? "" : k.trim();
        this.model = (m == null || m.isBlank()) ? "deepseek/deepseek-r1:free" : m.trim();

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(15000);
        this.rest = new RestTemplate(rf);
    }

    private String resolveEnv(Dotenv dotenv, String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String dv = dotenv.get(key);
        return (dv == null || dv.isBlank()) ? null : dv.trim();
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        if (apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "OPENROUTER_API_KEY no configurada"));
        }

        String prompt = safeStr(body.get("prompt"));
        String system = safeStr(body.get("system"));
        if (system.isBlank()) {
            system = "Eres un asistente de AgroLink. Responde en español con pasos claros y breves.";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-Title", "AgroLink");

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", system));
            messages.add(Map.of("role", "user", "content", prompt));
            payload.put("messages", messages);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
            String endpoint = baseUrl.replaceAll("/+$", "") + "/chat/completions";
            ResponseEntity<Map> resp = rest.postForEntity(endpoint, req, Map.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Fallo al obtener respuesta de OpenRouter"));
            }

            Map<String, Object> bodyMap = (Map<String, Object>) resp.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) bodyMap.get("choices");
            String text = "";
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg != null) {
                    Object content = msg.get("content");
                    text = content == null ? "" : content.toString();
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("output_text", text);
            result.put("usage", bodyMap.get("usage"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Excepción llamando a OpenRouter: " + e.getMessage()));
        }
    }

    private static String safeStr(Object o) { return o == null ? "" : o.toString().trim(); }
}