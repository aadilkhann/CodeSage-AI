package com.codesage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CorsConfig - Configure Cross-Origin Resource Sharing (CORS)
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why CORS?
 * - Frontend runs on different origin (http://localhost:5173)
 * - Backend runs on different origin (http://localhost:8080)
 * - Browser blocks cross-origin requests by default (security)
 * - We must explicitly allow frontend to call backend
 * 
 * 2. Security Considerations:
 * - Only allow specific origins (not *)
 * - Only allow necessary HTTP methods
 * - Allow credentials (cookies, Authorization header)
 * - Configure max age for preflight caching
 * 
 * 3. Preflight Requests:
 * - Browser sends OPTIONS request before actual request
 * - Checks if cross-origin request is allowed
 * - Caches result for max-age seconds
 * 
 * 4. Development vs Production:
 * - Development: Allow localhost:5173, localhost:3000
 * - Production: Only allow actual frontend domain
 * - Configure via application.yml
 */
@Configuration
public class CorsConfig {

    /**
     * Allowed origins from configuration
     * Example: "http://localhost:5173,http://localhost:3000"
     * Production: "https://yourdomain.com"
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Allowed HTTP methods
     * Example: "GET,POST,PUT,DELETE,OPTIONS"
     */
    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    /**
     * Allowed headers
     * Example: "*" or specific headers like "Authorization,Content-Type"
     */
    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    /**
     * Allow credentials (cookies, Authorization header)
     */
    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;

    /**
     * Configure CORS
     * 
     * THOUGHT PROCESS:
     * This bean is used by Spring Security to handle CORS
     * SecurityConfig needs this to enable CORS globally
     * 
     * Flow:
     * 1. Browser makes request to backend
     * 2. If cross-origin, browser sends OPTIONS preflight
     * 3. Backend responds with CORS headers
     * 4. If allowed, browser proceeds with actual request
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Step 1: Set allowed origins
        // Split comma-separated origins from application.yml
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Step 2: Set allowed HTTP methods
        // GET: Read data
        // POST: Create data
        // PUT: Update data
        // DELETE: Delete data
        // OPTIONS: Preflight requests
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);

        // Step 3: Set allowed headers
        // "*" allows all headers (simpler for development)
        // Production: Specify exact headers for security
        if ("*".equals(allowedHeaders)) {
            configuration.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers);
        }

        // Step 4: Allow credentials
        // True: Frontend can send cookies, Authorization header
        // Required for JWT in Authorization header
        configuration.setAllowCredentials(allowCredentials);

        // Step 5: Expose headers to frontend
        // Frontend JavaScript can read these response headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count" // For pagination
        ));

        // Step 6: Preflight cache duration
        // Browser caches preflight response for 1 hour
        // Reduces preflight requests (performance)
        configuration.setMaxAge(3600L);

        // Step 7: Apply configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
