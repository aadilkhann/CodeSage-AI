package com.codesage.config;

import com.codesage.security.JwtAuthenticationFilter;
import com.codesage.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig - Configure Spring Security
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Security Architecture:
 * - JWT-based authentication (stateless)
 * - OAuth2 for GitHub login
 * - No session management (REST API)
 * - CORS enabled for frontend
 * 
 * 2. Filter Chain:
 * Request → CORS → OAuth2 → JWT Filter → Controller
 * 
 * 3. Endpoint Security:
 * - Public: /api/v1/auth/**, /actuator/health
 * - Protected: All other /api/v1/** endpoints
 * 
 * 4. Why stateless?
 * - No server-side sessions
 * - Scales horizontally (multiple backend instances)
 * - Mobile-friendly (no cookies needed)
 * - Microservices-ready
 * 
 * 5. Security Flow:
 * - User logs in via GitHub OAuth
 * - Backend generates JWT tokens
 * - Frontend includes JWT in Authorization header
 * - JWT filter validates token on each request
 * - Controller receives authenticated user
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    /**
     * Configure security filter chain
     * 
     * THOUGHT PROCESS:
     * This is the heart of Spring Security configuration.
     * We define:
     * - Which endpoints are public vs protected
     * - How authentication works (OAuth2 + JWT)
     * - Session management (stateless)
     * - CORS settings
     * - Filter order
     * 
     * @param http HttpSecurity builder
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF
                // Why? We're using JWT (not cookies), so CSRF doesn't apply
                // CSRF protects against cookie-based attacks
                // JWT in Authorization header is not vulnerable to CSRF
                .csrf(csrf -> csrf.disable())

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/v1/auth/**", // Auth endpoints (login, callback, refresh)
                                "/api/v1/webhooks/**", // GitHub webhooks (verified by signature)
                                "/actuator/health", // Health check
                                "/actuator/info", // App info
                                "/error" // Error page
                        ).permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated())

                // Configure OAuth2 login
                .oauth2Login(oauth2 -> oauth2
                        // Where to redirect after successful GitHub login
                        .successHandler(oAuth2SuccessHandler)

                        // OAuth2 endpoints
                        .authorizationEndpoint(authorization -> authorization
                                // Frontend initiates login by redirecting to this endpoint
                                .baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(redirection -> redirection
                                // GitHub redirects here after user authorizes
                                .baseUri("/api/v1/auth/github/callback")))

                // Configure session management
                // STATELESS: No server-side sessions
                // Each request must include JWT token
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Add JWT filter before Spring Security's authentication filter
                // Order matters! JWT filter must run first to set authentication
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder bean
     * 
     * THOUGHT PROCESS:
     * - BCrypt is industry standard for password hashing
     * - We use it to hash GitHub access tokens before storing
     * - Why hash? If database is compromised, tokens are protected
     * - BCrypt automatically handles salt and multiple rounds
     * 
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
