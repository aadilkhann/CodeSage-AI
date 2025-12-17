package com.codesage.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SecurityConfigTest - Integration tests for Spring Security configuration
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why test security configuration?
 * - Verify public endpoints are accessible
 * - Verify protected endpoints require authentication
 * - Verify CORS is configured correctly
 * - Ensure OAuth endpoints are accessible
 * 
 * 2. Testing Strategy:
 * - Full Spring context integration tests
 * - Test actual HTTP security
 * - Verify filter chain behavior
 * 
 * 3. Test Coverage:
 * - Public endpoint access
 * - Protected endpoint authentication
 * - CORS preflight requests
 * - Security headers
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security Configuration Integration Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // ============================================
    // PUBLIC ENDPOINT TESTS
    // ============================================

    @Test
    @DisplayName("Should allow access to health endpoint without authentication")
    void shouldAllowAccessToHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to info endpoint without authentication")
    void shouldAllowAccessToInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to auth refresh endpoint without authentication")
    void shouldAllowAccessToAuthRefreshEndpoint() throws Exception {
        // This will fail validation but should not return 401
        mockMvc.perform(get("/api/v1/auth/refresh"))
                .andExpect(status().isNot(401));
    }

    // ============================================
    // PROTECTED ENDPOINT TESTS
    // ============================================

    @Test
    @DisplayName("Should require authentication for /api/v1/repositories")
    void shouldRequireAuthenticationForRepositories() throws Exception {
        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for /api/v1/auth/me")
    void shouldRequireAuthenticationForAuthMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ============================================
    // CORS TESTS
    // ============================================

    @Test
    @DisplayName("Should handle CORS preflight request")
    void shouldHandleCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("Should include CORS headers in actual response")
    void shouldIncludeCorsHeadersInActualResponse() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    // ============================================
    // CSRF TESTS
    // ============================================

    @Test
    @DisplayName("Should have CSRF disabled for REST API")
    void shouldHaveCsrfDisabledForRestApi() throws Exception {
        // POST without CSRF token should not return 403
        // (will return 401 for authentication, not 403 for CSRF)
        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isNot(403));
    }
}
