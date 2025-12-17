package com.codesage.controller;

import com.codesage.dto.LoginResponse;
import com.codesage.dto.TokenRefreshRequest;
import com.codesage.dto.UserDTO;
import com.codesage.security.JwtTokenProvider;
import com.codesage.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthControllerTest - Integration tests for authentication endpoints
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why integration tests?
 * - Test full HTTP request/response cycle
 * - Verify security configuration
 * - Test JSON serialization/deserialization
 * - Validate status codes and headers
 * 
 * 2. Testing Strategy:
 * - Use @WebMvcTest for controller layer testing
 * - Mock service layer
 * - Test with Spring Security context
 * - Verify request/response mapping
 * 
 * 3. Test Coverage:
 * - POST /api/v1/auth/refresh - success and failure
 * - GET /api/v1/auth/me - authenticated and unauthenticated
 * - POST /api/v1/auth/logout
 */
@WebMvcTest(AuthController.class)
@DisplayName
(\"Auth Controller Integration Tests\")class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    // Test data
    private UUID testUserId;
    private UserDTO testUserDTO;
    private LoginResponse loginResponse;
    private String validRefreshToken;
    private String invalidRefreshToken;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Create test user DTO
        testUserDTO = new UserDTO();
        testUserDTO.setId(testUserId);
        testUserDTO.setUsername(\"testuser\");
        testUserDTO.setEmail(\"test@example.com\");
        testUserDTO.setAvatarUrl(\"https://github.com/avatar.png\");
        testUserDTO.setGithubId(12345678L);
        testUserDTO.setCreatedAt(LocalDateTime.now());

        // Create login response
        loginResponse = new LoginResponse(
            \"new.access.token\",
            \"refresh.token\",
            3600L,
            testUserDTO
        );

        validRefreshToken = \"valid.refresh.token\";
        invalidRefreshToken = \"invalid.token\";
    }

    // ============================================
    // TOKEN REFRESH ENDPOINT TESTS
    // ============================================

    @Test
    @DisplayName(\"POST /api/v1/auth/refresh - Should refresh token successfully\")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest(validRefreshToken);
        when(authService.refreshAccessToken(validRefreshToken)).thenReturn(loginResponse);

        // When/Then
        mockMvc.perform(post(\"/api/v1/auth/refresh\")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(\"$.accessToken\").value(\"new.access.token\"))
                .andExpect(jsonPath(\"$.refreshToken\").value(\"refresh.token\"))
                .andExpect(jsonPath(\"$.tokenType\").value(\"Bearer\"))
                .andExpect(jsonPath(\"$.expiresIn\").value(3600))
                .andExpect(jsonPath(\"$.user.id\").value(testUserId.toString()))
                .andExpect(jsonPath(\"$.user.username\").value(\"testuser\"))
                .andExpect(jsonPath(\"$.user.email\").value(\"test@example.com\"));
    }

    @Test
    @DisplayName(\"POST /api/v1/auth/refresh - Should return 400 for invalid token\")
    void shouldReturn400ForInvalidRefreshToken() throws Exception {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest(invalidRefreshToken);
        when(authService.refreshAccessToken(invalidRefreshToken))
                .thenThrow(new RuntimeException(\"Invalid refresh token\"));

        // When/Then
        mockMvc.perform(post(\"/api/v1/auth/refresh\")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(\"POST /api/v1/auth/refresh - Should return 400 for empty token\")
    void shouldReturn400ForEmptyRefreshToken() throws Exception {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest(\"\");

        // When/Then
        mockMvc.perform(post(\"/api/v1/auth/refresh\")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============================================
    // GET CURRENT USER ENDPOINT TESTS
    // ============================================

    @Test
    @WithMockUser(username = \"user-id-string\")
    @DisplayName(\"GET /api/v1/auth/me - Should return current user when authenticated\")
    void shouldReturnCurrentUserWhenAuthenticated() throws Exception {
        // Given
        when(authService.getCurrentUser(any(UUID.class))).thenReturn(testUserDTO);

        // When/Then
        mockMvc.perform(get(\"/api/v1/auth/me\")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(\"$.username\").value(\"testuser\"))
                .andExpect(jsonPath(\"$.email\").value(\"test@example.com\"))
                .andExpect(jsonPath(\"$.githubId\").value(12345678));
    }

    @Test
    @DisplayName(\"GET /api/v1/auth/me - Should return 401 when unauthenticated\")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        // When/Then
        mockMvc.perform(get(\"/api/v1/auth/me\"))
                .andExpect(status().isUnauthorized());
    }

    // ============================================
    // LOGOUT ENDPOINT TESTS
    // ============================================

    @Test
    @WithMockUser
    @DisplayName(\"POST /api/v1/auth/logout - Should logout successfully\")
    void shouldLogoutSuccessfully() throws Exception {
        // When/Then
        mockMvc.perform(post(\"/api/v1/auth/logout\")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(\"$.message\").value(\"Logged out successfully\"));
    }

    @Test
    @DisplayName(\"POST /api/v1/auth/logout - Should return 401 when unauthenticated\")
    void shouldReturn401WhenLoggingOutUnauthenticated() throws Exception {
        // When/Then
        mockMvc.perform(post(\"/api/v1/auth/logout\"))
                .andExpect(status().isUnauthorized());
    }

    // ============================================
    // REQUEST VALIDATION TESTS
    // ============================================

    @Test
    @DisplayName(\"POST /api/v1/auth/refresh - Should validate request body\")
    void shouldValidateRefreshRequestBody() throws Exception {
        // When/Then: Send request with missing refreshToken field
        mockMvc.perform(post(\"/api/v1/auth/refresh\")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(\"{}\"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(\"POST /api/v1/auth/refresh - Should reject malformed JSON\")
    void shouldRejectMalformedJson() throws Exception {
        // When/Then
        mockMvc.perform(post(\"/api/v1/auth/refresh\")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(\"not valid json\"))
                .andExpect(status().isBadRequest());
    }
}
