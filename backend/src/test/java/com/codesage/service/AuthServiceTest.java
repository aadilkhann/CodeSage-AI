package com.codesage.service;

import com.codesage.dto.LoginResponse;
import com.codesage.dto.UserDTO;
import com.codesage.model.User;
import com.codesage.repository.UserRepository;
import com.codesage.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthServiceTest - Unit tests for authentication service
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why test AuthService?
 * - Core business logic for authentication
 * - Token refresh security-critical
 * - User data handling
 * 
 * 2. Testing Strategy:
 * - Mock dependencies (UserRepository, JwtTokenProvider)
 * - Test business logic in isolation
 * - Verify error handling
 * 
 * 3. Test Coverage:
 * - Token refresh success
 * - Token refresh failures (invalid, expired, wrong type)
 * - Get current user
 * - User not found scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName
(\"Auth Service Tests\")class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    // Test data
    private User testUser;
    private UUID testUserId;
    private String validRefreshToken;
    private String validAccessToken;
    private String expiredRefreshToken;
    private String accessTokenUsedAsRefresh;

    @BeforeEach
    void setUp() {
        // Set access token expiration via reflection
        ReflectionTestUtils.setField(authService, \"accessTokenExpiration\", 3600000L);

        // Initialize test data
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setGithubId(12345678L);
        testUser.setUsername(\"testuser\");
        testUser.setEmail(\"test@example.com\");
        testUser.setAvatarUrl(\"https://github.com/avatar.png\");
        testUser.setCreatedAt(LocalDateTime.now());

        // Mock tokens
        validRefreshToken = \"valid.refresh.token\";
        validAccessToken = \"valid.access.token\";
        expiredRefreshToken = \"expired.refresh.token\";
        accessTokenUsedAsRefresh = \"access.token.wrongtype\";
    }

    // ============================================
    // TOKEN REFRESH TESTS - SUCCESS CASES
    // ============================================

    @Test
    @DisplayName(\"Should successfully refresh access token with valid refresh token\")
    void shouldRefreshAccessTokenSuccessfully() {
        // Given
        when(tokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(validRefreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(validRefreshToken)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(testUserId, testUser.getUsername(), testUser.getGithubId()))
                .thenReturn(validAccessToken);

        // When
        LoginResponse response = authService.refreshAccessToken(validRefreshToken);

        // Then
        assertNotNull(response, \"Response should not be null\");
        assertEquals(validAccessToken, response.getAccessToken(), \"Should return new access token\");
        assertEquals(validRefreshToken, response.getRefreshToken(), \"Should return same refresh token\");
        assertEquals(\"Bearer\", response.getTokenType(), \"Token type should be Bearer\");
        assertEquals(3600, response.getExpiresIn(), \"Expiration should be 3600 seconds\");
        
        assertNotNull(response.getUser(), \"User should not be null\");
        assertEquals(testUserId, response.getUser().getId(), \"User ID should match\");
        assertEquals(testUser.getUsername(), response.getUser().getUsername(), \"Username should match\");

        // Verify interactions
        verify(tokenProvider).validateToken(validRefreshToken);
        verify(tokenProvider).isRefreshToken(validRefreshToken);
        verify(tokenProvider).getUserIdFromToken(validRefreshToken);
        verify(userRepository).findById(testUserId);
        verify(tokenProvider).generateAccessToken(testUserId, testUser.getUsername(), testUser.getGithubId());
    }

    // ============================================
    // TOKEN REFRESH TESTS - FAILURE CASES
    // ============================================

    @Test
    @DisplayName(\"Should reject invalid refresh token\")
    void shouldRejectInvalidRefreshToken() {
        // Given
        when(tokenProvider.validateToken(\"invalid.token\")).thenReturn(false);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshAccessToken(\"invalid.token\");
        });

        assertEquals(\"Invalid refresh token\", exception.getMessage());
        verify(tokenProvider).validateToken(\"invalid.token\");
        verify(tokenProvider, never()).isRefreshToken(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName(\"Should reject access token used as refresh token\")
    void shouldRejectAccessTokenUsedAsRefreshToken() {
        // Given
        when(tokenProvider.validateToken(accessTokenUsedAsRefresh)).thenReturn(true);
        when(tokenProvider.isRefreshToken(accessTokenUsedAsRefresh)).thenReturn(false);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshAccessToken(accessTokenUsedAsRefresh);
        });

        assertEquals(\"Invalid token type\", exception.getMessage());
        verify(tokenProvider).validateToken(accessTokenUsedAsRefresh);
        verify(tokenProvider).isRefreshToken(accessTokenUsedAsRefresh);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName(\"Should reject refresh token for non-existent user\")
    void shouldRejectRefreshTokenForNonExistentUser() {
        // Given
        when(tokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(validRefreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(validRefreshToken)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshAccessToken(validRefreshToken);
        });

        assertEquals(\"User not found\", exception.getMessage());
        verify(userRepository).findById(testUserId);
        verify(tokenProvider, never()).generateAccessToken(any(), any(), any());
    }

    // ============================================
    // GET CURRENT USER TESTS
    // ============================================

    @Test
    @DisplayName(\"Should return current user successfully\")
    void shouldReturnCurrentUserSuccessfully() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserDTO userDTO = authService.getCurrentUser(testUserId);

        // Then
        assertNotNull(userDTO, \"User DTO should not be null\");
        assertEquals(testUserId, userDTO.getId(), \"User ID should match\");
        assertEquals(testUser.getUsername(), userDTO.getUsername(), \"Username should match\");
        assertEquals(testUser.getEmail(), userDTO.getEmail(), \"Email should match\");
        assertEquals(testUser.getAvatarUrl(), userDTO.getAvatarUrl(), \"Avatar URL should match\");
        assertEquals(testUser.getGithubId(), userDTO.getGithubId(), \"GitHub ID should match\");
        assertEquals(testUser.getCreatedAt(), userDTO.getCreatedAt(), \"Created at should match\");

        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName(\"Should throw exception when getting non-existent user\")
    void shouldThrowExceptionWhenGettingNonExistentUser() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.getCurrentUser(nonExistentUserId);
        });

        assertEquals(\"User not found\", exception.getMessage());
        verify(userRepository).findById(nonExistentUserId);
    }

    // ============================================
    // DTO CONVERSION TESTS
    // ============================================

    @Test
    @DisplayName(\"Should convert user entity to DTO correctly\")
    void shouldConvertUserEntityToDtoCorrectly() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserDTO dto = authService.getCurrentUser(testUserId);

        // Then
        assertAll(\"User DTO fields\",
            () -> assertEquals(testUser.getId(), dto.getId()),
            () -> assertEquals(testUser.getUsername(), dto.getUsername()),
            () -> assertEquals(testUser.getEmail(), dto.getEmail()),
            () -> assertEquals(testUser.getAvatarUrl(), dto.getAvatarUrl()),
            () -> assertEquals(testUser.getGithubId(), dto.getGithubId()),
            () -> assertEquals(testUser.getCreatedAt(), dto.getCreatedAt())
        );
    }

    @Test
    @DisplayName(\"Should handle user with null email\")
    void shouldHandleUserWithNullEmail() {
        // Given
        testUser.setEmail(null);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserDTO dto = authService.getCurrentUser(testUserId);

        // Then
        assertNull(dto.getEmail(), \"Email should be null\");
        assertNotNull(dto.getUsername(), \"Username should not be null\");
    }
}
