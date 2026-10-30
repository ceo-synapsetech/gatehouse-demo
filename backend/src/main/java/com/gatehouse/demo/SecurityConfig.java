package com.gatehouse.demo;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("guard01").password(encoder.encode("demo123")).roles("SECURITY").build(),
            User.withUsername("ananya@example.com").password(encoder.encode("demo123")).roles("RESIDENT").build()
        );
    }

    @Bean UrlBasedCorsConfigurationSource cors(@Value("${CORS_ALLOWED_ORIGIN:https://gatehouse-demo.pages.dev,https://gatehouse.synapsetechworld.com,https://gatehouse-keep.synapsetechworld.com,https://gatehouse-res.synapsetechworld.com}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean SecurityFilterChain security(HttpSecurity http, UrlBasedCorsConfigurationSource corsSource) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsSource)).csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.requestMatchers("/api/health", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/requests").hasRole("SECURITY")
                .requestMatchers(HttpMethod.PATCH, "/api/requests/*/decision").hasRole("RESIDENT")
                .requestMatchers("/api/**").authenticated())
            .httpBasic(Customizer.withDefaults()).build();
    }
}