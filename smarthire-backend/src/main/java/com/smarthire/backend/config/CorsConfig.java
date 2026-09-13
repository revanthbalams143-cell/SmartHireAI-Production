package com.smarthire.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500}") String allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        String trimmedOrigins = allowedOrigins == null ? "" : allowedOrigins.trim();
        if ("*".equals(trimmedOrigins)) {
            throw new IllegalStateException("Wildcard CORS is not permitted when credentials are enabled. Set APP_CORS_ALLOWED_ORIGINS to explicit origins.");
        } else {
            List<String> origins = Arrays.stream(trimmedOrigins.split(","))
                    .map(String::trim)
                    .filter(o -> !o.isEmpty())
                    .toList();
            if (origins.isEmpty()) {
                throw new IllegalStateException(
                        "app.cors.allowed-origins resolved to no valid origins. " +
                        "Set APP_CORS_ALLOWED_ORIGINS to a comma-separated list of allowed origins.");
            }
            config.setAllowedOrigins(origins);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
