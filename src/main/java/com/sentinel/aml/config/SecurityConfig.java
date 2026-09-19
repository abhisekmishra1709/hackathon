package com.sentinel.aml.config;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "timestamp", Instant.now(), "status", 401,
                                    "error", "Unauthorized", "message", "Authentication is required"));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "timestamp", Instant.now(), "status", 403,
                                    "error", "Forbidden", "message", "Insufficient permissions"));
                        }))
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                        "/api/v1/system/status",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/api/v1/ingestion/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/rules/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/transactions/**").hasAnyRole("ADMIN", "INGESTOR")
                        .requestMatchers("/api/v1/audit-events/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/alerts/**", "/api/v1/cases/**")
                    .hasAnyRole("ADMIN", "ANALYST")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

            @Bean
            UserDetailsService userDetailsService(SecurityUserProperties properties, PasswordEncoder passwordEncoder) {
            return new InMemoryUserDetailsManager(
                User.withUsername(properties.getAdminUsername())
                    .password(passwordEncoder.encode(properties.getAdminPassword()))
                    .roles("ADMIN", "ANALYST", "INGESTOR")
                    .build(),
                User.withUsername(properties.getAnalystUsername())
                    .password(passwordEncoder.encode(properties.getAnalystPassword()))
                    .roles("ANALYST")
                    .build());
            }

            @Bean
            PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
            }
}