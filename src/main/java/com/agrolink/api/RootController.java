package com.agrolink.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Professional landing endpoint for the API root. Instead of a Whitelabel error
 * page, we return structured metadata plus helpful links. Optionally you can
 * change this to a redirect to Swagger UI.
 */
@RestController
public class RootController {

    @Value("${spring.application.name:agrolink-backend}")
    private String appName;

    @Value("${info.app.version:0.0.1-SNAPSHOT}")
    private String version;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "name", appName,
                "version", version,
                "timestamp", Instant.now().toString(),
                "status", "OK",
                "documentation", "/swagger-ui.html",
                "apiBase", "/api/v1",
                "message", "Bienvenido a la API de AgroLink. Explora la documentación en /swagger-ui.html y los endpoints bajo /api/v1"
        ));
    }
}
