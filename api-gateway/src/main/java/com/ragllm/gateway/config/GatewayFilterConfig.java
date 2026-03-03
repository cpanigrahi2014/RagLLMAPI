package com.ragllm.gateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global filters for request/response logging and CORS.
 */
@Configuration
public class GatewayFilterConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayFilterConfig.class);

    /**
     * Explicit CorsWebFilter to guarantee CORS headers on ALL responses,
     * including preflight OPTIONS that may be short-circuited by rate limiters.
     */
    @Bean
    @Order(-2)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    @Order(-1)
    public GlobalFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getURI().getPath();

            log.info("Gateway request: {} {}", method, path);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 0;
                log.info("Gateway response: {} {} -> {} ({}ms)", method, path, statusCode, duration);
            }));
        };
    }
}
