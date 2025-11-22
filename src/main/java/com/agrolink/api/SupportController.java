package com.agrolink.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@RestController
@RequestMapping("/api/v1/ai/assist")
public class SupportController {

    private final RestTemplate rest;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String referrer;
    private final String siteTitle;
    private final String modelFallback;

    public SupportController() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String b = getenv(dotenv, "OPENROUTER_BASE_URL");
        String k = getenv(dotenv, "OPENROUTER_API_KEY");
        String m = getenv(dotenv, "OPENROUTER_MODEL");
        String r = getenv(dotenv, "OPENROUTER_REFERRER");
        String t = getenv(dotenv, "OPENROUTER_TITLE");
        this.baseUrl = (b == null || b.isBlank()) ? "https://openrouter.ai/api/v1" : b.trim();
        this.apiKey = (k == null) ? "" : k.trim();
        this.model = (m == null || m.isBlank()) ? "deepseek/deepseek-r1:free" : m.trim();
        this.modelFallback = getenv(dotenv, "OPENROUTER_MODEL_FALLBACK");
        this.referrer = (r == null) ? "" : r.trim();
        this.siteTitle = (t == null || t.isBlank()) ? "AgroLink" : t.trim();

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(15000);
        this.rest = new RestTemplate(rf);
    }

    private static String getenv(Dotenv dotenv, String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String dv = dotenv.get(key);
        return (dv == null || dv.isBlank()) ? null : dv.trim();
    }

    @PostMapping("/support")
    public ResponseEntity<Map<String,Object>> support(@RequestBody Map<String,Object> body) {
        if (apiKey.isBlank()) {
            String question = safeStr(body.get("question"));
            String out = fallbackAnswer(question);
            return ResponseEntity.ok(Map.of("output_text", out));
        }

        String userName = safeStr(body.get("userName"));
        String question = safeStr(body.get("question"));
        List<Map<String,String>> history = parseHistory(body.get("history"));
        if (history == null) history = java.util.Collections.emptyList();

        String system = (
                "Eres un asistente de AgroLink para campesinos y compradores. " +
                "Responde en español, claro y breve (máx. 4 líneas), con pasos concretos. " +
                "Si te preguntan sobre el flujo de compra o pedidos, guía: cantidad → precio → entrega → pago. " +
                "Evita tecnicismos. Si falta información, pide lo mínimo."
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-Title", siteTitle);
            if (!referrer.isBlank()) headers.set("HTTP-Referer", referrer);

            List<Map<String,String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", system));
            if (userName != null && !userName.isBlank()) {
                messages.add(Map.of("role", "system", "content", "Usuario: " + userName));
            }
            if (history != null && !history.isEmpty()) messages.addAll(history);
            if (question != null && !question.isBlank()) {
                messages.add(Map.of("role", "user", "content", question));
            } else if (history.isEmpty()) {
                messages.add(Map.of("role", "user", "content", "Hola"));
            }

            Map<String,Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("messages", messages);

            HttpEntity<Map<String,Object>> req = new HttpEntity<>(payload, headers);
            String endpoint = baseUrl.replaceAll("/+$$", "") + "/chat/completions";
            try {
                ResponseEntity<Map<String,Object>> resp = rest.exchange(
                        endpoint,
                        HttpMethod.POST,
                        req,
                        new ParameterizedTypeReference<Map<String,Object>>() {}
                );

                Map<String,Object> respBody = resp.getBody();
                if (!resp.getStatusCode().is2xxSuccessful() || respBody == null) {
                    throw new RestClientResponseException("Bad gateway", resp.getStatusCode().value(), "Bad Gateway", null, null, null);
                }

                Object choicesObj = respBody.get("choices");
                String text = "";
                if (choicesObj instanceof List) {
                    List<?> choices = (List<?>) choicesObj;
                    if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                        Map<?,?> msg = (Map<?,?>)((Map<?,?>) choices.get(0)).get("message");
                        Object content = msg == null ? null : msg.get("content");
                        text = content == null ? "" : content.toString();
                    }
                }

                Map<String,Object> out = new HashMap<>();
                out.put("output_text", text);
                out.put("usage", respBody.get("usage"));
                return ResponseEntity.ok(out);
            } catch (RestClientResponseException ex) {
                int code = ex.getRawStatusCode();
                boolean rateLimited = code == 429;
                boolean serverError = code >= 500;
                if ((rateLimited || serverError) && modelFallback != null && !modelFallback.isBlank()) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    payload.put("model", modelFallback.trim());
                    ResponseEntity<Map<String,Object>> resp2 = rest.exchange(
                            endpoint,
                            HttpMethod.POST,
                            new HttpEntity<>(payload, headers),
                            new ParameterizedTypeReference<Map<String,Object>>() {}
                    );
                    Map<String,Object> body2 = resp2.getBody();
                    if (resp2.getStatusCode().is2xxSuccessful() && body2 != null) {
                        Object choicesObj = body2.get("choices");
                        String text = "";
                        if (choicesObj instanceof List) {
                            List<?> choices = (List<?>) choicesObj;
                            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                                Map<?,?> msg = (Map<?,?>)((Map<?,?>) choices.get(0)).get("message");
                                Object content = msg == null ? null : msg.get("content");
                                text = content == null ? "" : content.toString();
                            }
                        }
                        Map<String,Object> out = new HashMap<>();
                        out.put("output_text", text);
                        out.put("usage", body2.get("usage"));
                        out.put("model_used", modelFallback.trim());
                        return ResponseEntity.ok(out);
                    }
                }
                String out = fallbackAnswer(question) + "\n(Alta demanda de IA, intenta nuevamente en un momento)";
                return ResponseEntity.ok(Map.of("output_text", out));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Excepción llamando a OpenRouter: " + e.getMessage()));
        }
    }

    private static String safeStr(Object o) { return o == null ? "" : o.toString().trim(); }
    private static String fallbackAnswer(String q){
        String s = (q==null?"":q.toLowerCase());
        if (s.contains("cargar") && s.contains("producto")) {
            return "1) Entra a Productos > Publicar.\n2) Escribe nombre y precio.\n3) Agrega fotos desde tu celular.\n4) Guarda para publicar.";
        }
        if (s.contains("fotos") || s.contains("imagenes") || s.contains("imágenes")) {
            return "1) Abre Publicar producto.\n2) Pulsa Agregar fotos.\n3) Selecciona 3–5 fotos claras.\n4) Guarda los cambios.";
        }
        if (s.contains("descrip") ) {
            return "1) Di qué vendes y cantidad.\n2) Precio y calidad (fresco/orgánico).\n3) Lugar y entrega.\n4) Evita textos largos.";
        }
        if (s.contains("pedido") || s.contains("compra") ) {
            return "1) Cantidad acordada.\n2) Precio final.\n3) Entrega (encuentro/domicilio/vereda).\n4) Pago (Nequi/Daviplata/contraentrega).";
        }
        return "Hola, ¿en qué puedo ayudarte? Indica si es sobre publicar, descripción, pedidos o pagos.";
    }
    private static List<Map<String,String>> parseHistory(Object o){
        if (!(o instanceof List)) return Collections.emptyList();
        List<?> arr = (List<?>) o;
        List<Map<String,String>> out = new ArrayList<>();
        for (Object x : arr) {
            if (x instanceof Map) {
                Map<?,?> m = (Map<?,?>) x;
                Object role = m.get("role");
                Object content = m.get("content");
                if (role!=null && content!=null) {
                    out.add(Map.of("role", role.toString(), "content", content.toString()));
                }
            }
        }
        return out;
    }
}
