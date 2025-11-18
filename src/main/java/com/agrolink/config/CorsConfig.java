package com.agrolink.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull final CorsRegistry registry) {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                String frontProd = orDefault(dotenv.get("FRONTEND_ORIGIN"), "https://agro-link-jet.vercel.app");
                String frontDev = orDefault(dotenv.get("FRONTEND_ORIGIN_DEV"), "http://localhost:5174");

                registry.addMapping("/api/**")
                        .allowedOrigins(frontProd, frontDev)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    private static String orDefault(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}
