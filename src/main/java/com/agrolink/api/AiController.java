package com.agrolink.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

@RestController
@RequestMapping("/api/v1/ai/assist")
public class AiController {
    private final RestTemplate rest;
    private final String apiKey;

    public AiController() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) key = dotenv.get("OPENAI_API_KEY");
        this.apiKey = key == null ? "" : key.trim();

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(15000);
        this.rest = new RestTemplate(rf);
    }

    @PostMapping("/guide")
    public ResponseEntity<Map<String, Object>> guide(@RequestBody Map<String, Object> body) {
        if (apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "OPENAI_API_KEY no configurada"));
        }

        String userName = safeStr(body.get("userName"));
        String question = safeStr(body.get("question"));

        String system = "Eres un asistente de AgroLink para campesinos de habla hispana. " +
                "Guiarás paso a paso: crear perfil, agregar ubicación y productos, y cómo vender. " +
                "Usa lenguaje sencillo, empático y oraciones cortas. Ofrece opciones concretas de siguientes pasos. " +
                "Si el usuario pide ayuda, da instrucciones claras y numeradas.";

        String userPrompt = (question != null && !question.isBlank()) ? question : (
                (userName.isBlank() ? "" : ("Me llamo " + userName + ". ")) +
                "Acabo de registrarme por primera vez. Explícame qué debo hacer ahora mismo para: \n" +
                "1) Completar mi perfil (nombre, ubicación), \n" +
                "2) Publicar un producto, \n" +
                "3) Recibir y responder mensajes, \n" +
                "4) Concretar la venta de forma segura."
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "gpt-4o-mini");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", system));
            messages.add(Map.of("role", "user", "content", userPrompt));
            payload.put("messages", messages);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> resp = rest.postForEntity("https://api.openai.com/v1/chat/completions", req, Map.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Fallo al obtener respuesta de OpenAI"));
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
                    .body(Map.of("error", "Excepción llamando a OpenAI: " + e.getMessage()));
        }
    }

    @PostMapping("/support")
    public ResponseEntity<Map<String, Object>> support(@RequestBody Map<String, Object> body) {
        if (apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "OPENAI_API_KEY no configurada"));
        }

        String userName = safeStr(body.get("userName"));
        String question = safeStr(body.get("question"));

        String system = "Eres soporte de AgroLink para campesinos. Responde en español con pasos claros y breves, tono amable, y ejemplos simples. Ayuda en: cargar productos, fotos, descripciones, y gestión de pedidos. Si conviene, lista pasos numerados.";
        String userPrompt = (userName.isBlank() ? "" : ("Me llamo " + userName + ". ")) + (question.isBlank() ? "Necesito ayuda usando AgroLink." : question);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "gpt-4o-mini");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", system));
            messages.add(Map.of("role", "user", "content", userPrompt));
            payload.put("messages", messages);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> resp = rest.postForEntity("https://api.openai.com/v1/chat/completions", req, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Fallo al obtener respuesta de OpenAI"));
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
                    .body(Map.of("error", "Excepción llamando a OpenAI: " + e.getMessage()));
        }
    }

    private static String safeStr(Object o) { return o == null ? "" : o.toString().trim(); }
}
