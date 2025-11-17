package com.safra.safra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // configure CORS using the CorsConfigurationSource bean below (non-deprecated)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // disable CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // authorization rules
                .authorizeHttpRequests(auth -> auth
                        // allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // public auth endpoints
                        .requestMatchers("/auth/**").permitAll()
                        // secure admin endpoints (match your controller path)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // everything else authenticated
                        .anyRequest().authenticated()
                )
                // stateless session (JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // add jwt filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // for development allow localhost:4200. For production list explicit origins.
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));
        // allow Authorization header because frontend sends Bearer token
        config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        // if you use cookies or credentials, keep true (and do NOT use "*")
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}