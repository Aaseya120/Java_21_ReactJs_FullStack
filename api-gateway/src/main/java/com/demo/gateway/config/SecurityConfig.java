package com.demo.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the API Gateway.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>CSRF is disabled — this is a stateless REST gateway using JWT; no session cookies.</li>
 *   <li>All route authorization is handled by {@code AuthenticationFilter} (JWT validation).</li>
 *   <li>CORS is configured here (in addition to gateway YAML) so Spring Security does not
 *       reject preflight OPTIONS requests before they reach the gateway router.</li>
 * </ul>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // ── CSRF: disabled for stateless JWT API ────────────
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // ── CORS: handled via CorsConfigurationSource bean ───
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── HTTP Basic / Form login: not used ───────────────
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // ── Authorization: permit all — JWT validation is
                //    done by AuthenticationFilter GlobalFilter ──────
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow the React dev server and any other configured origins
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Idempotency-Key",
                "Idempotency-Key",
                "X-Auth-User",
                "X-Auth-Role"
        ));
        config.setExposedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                "X-Auth-User",
                "X-Auth-Role"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
