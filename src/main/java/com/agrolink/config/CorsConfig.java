package com.agrolink.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Removed WebMvcConfigurer CORS; using only global CorsFilter for consistency.
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Optional;

@Configuration
public class CorsConfig {

    // (WebMvcConfigurer removed)

    // CorsFilter global para asegurar CORS también en respuestas de error antes de llegar a MVC
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter globalCorsFilter() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String frontProd = resolveEnv(dotenv, "FRONTEND_ORIGIN").orElse("https://agro-link-jet.vercel.app");
        String frontDev = resolveEnv(dotenv, "FRONTEND_ORIGIN_DEV").orElse("http://localhost:5173");

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.addAllowedOriginPattern(frontProd);
        cfg.addAllowedOriginPattern(frontDev);
        cfg.addAllowedOriginPattern("https://*.vercel.app");
        cfg.addAllowedHeader(CorsConfiguration.ALL);
        cfg.addExposedHeader("Content-Disposition");
        cfg.addAllowedMethod("GET");
        cfg.addAllowedMethod("POST");
        cfg.addAllowedMethod("PUT");
        cfg.addAllowedMethod("PATCH");
        cfg.addAllowedMethod("DELETE");
        cfg.addAllowedMethod("OPTIONS");
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        source.registerCorsConfiguration("/actuator/**", cfg);
        source.registerCorsConfiguration("/**", cfg); // última red de seguridad
        return new CorsFilter(source);
    }

    private static Optional<String> resolveEnv(Dotenv dotenv, String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return Optional.of(sys.trim());
        String dv = dotenv.get(key);
        if (dv != null && !dv.isBlank()) return Optional.of(dv.trim());
        return Optional.empty();
    }
}
