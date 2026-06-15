package com.kumple.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final String[] allowedOrigins;
    private final String frontendUrl;

    public SecurityConfig(
            @Value("${app.allowed-origins}") String[] allowedOrigins,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.allowedOrigins = allowedOrigins;
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error", "/login/**", "/oauth2/**", "/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rooms/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rooms/*/game").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rounds/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rounds/*/classic-setup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms/*/join").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms/*/leave").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms/*/close").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rounds/*/submit-question").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rounds/*/submit-answer").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rounds/*/expire-time").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms/*/settings").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms/*/start").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms/*/rounds/next").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rooms/*/reset-lobby").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl(frontendUrl + "/create-room", true))
            .logout(logout -> logout.logoutSuccessUrl(frontendUrl));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
