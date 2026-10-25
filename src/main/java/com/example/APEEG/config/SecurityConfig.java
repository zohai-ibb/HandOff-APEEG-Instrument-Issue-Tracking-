package com.example.APEEG.config;

import com.example.APEEG.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Enable CORS using the custom CorsConfigurationSource bean below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Disable CSRF protection for stateless REST APIs
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Configure stateless session management (JWT-based)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Endpoint Access Control
                .authorizeHttpRequests(auth -> auth
                        // Permit all OPTIONS preflight requests globally
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Permit public authentication & login routes
                        .requestMatchers("/api/auth/**", "/api/persons/login").permitAll()

                        // Permit public access to static file uploads (images)
                        .requestMatchers("/uploads/**").permitAll()

                        // All other operational domain endpoints require a valid JWT
                        .anyRequest().authenticated()
                )

                // 5. Inject custom JWT filter before Spring's authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Whitelist all Capacitor mobile origins, local web dev servers, and live Render backend domains
        configuration.setAllowedOrigins(List.of(
                "https://localhost",                                   // Capacitor Android HTTPS Default Scheme
                "http://localhost",                                    // Capacitor Android HTTP Scheme
                "capacitor://localhost",                               // Capacitor Native Scheme
                "http://localhost:5173",                               // Vite Local Dev
                "http://localhost:3000",                               // React Local Dev
                "https://csir-yantra-dhristi-2jpl.onrender.com"             // Live Render Domain (without h)
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}