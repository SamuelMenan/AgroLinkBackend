package com.agrolink.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

import java.util.Optional;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull final CorsRegistry registry) {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                String frontProd = resolveEnv(dotenv, "FRONTEND_ORIGIN").orElse("https://agro-link-jet.vercel.app");
                String frontDev = resolveEnv(dotenv, "FRONTEND_ORIGIN_DEV").orElse("http://localhost:5173");

                // Principal API
                registry.addMapping("/api/**")
                        .allowedOrigins(frontProd, frontDev)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Disposition")
                        .allowCredentials(true);

                // Actuator y salud (para warm‑up y monitoreo desde frontend / edge)
                registry.addMapping("/actuator/**")
                        .allowedOrigins(frontProd, frontDev)
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false);
            }
        };
    }

    private static Optional<String> resolveEnv(Dotenv dotenv, String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return Optional.of(sys.trim());
        String dv = dotenv.get(key);
        if (dv != null && !dv.isBlank()) return Optional.of(dv.trim());
        return Optional.empty();
    }
}
