package com.codesage.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProviderTest - Unit tests for JWT token generation and validation
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why unit tests for JWT?
 * - Security-critical component
 * - Must verify tokens are generated correctly
 * - Must verify validation catches invalid tokens
 * - Must verify expiration handling
 * 
 * 2. Test Coverage:
 * - Token generation (access and refresh)
 * - Token validation (valid, expired, malformed)
 * - Claims extraction (userId, username, githubId)
 * - Token type discrimination
 * - Edge cases (null, empty, tampered tokens)
 * 
 * 3. Testing Strategy:
 * - Use ReflectionTestUtils to set private fields
 * - Generate real tokens, not mocks
 * - Test both happy path and error cases
 */
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    // Test data
    private UUID testUserId;
    private String testUsername;
    private Long testGithubId;

    // JWT configuration
    private static final String TEST_JWT_SECRET = \"testSecretKeyThatIsAtLeast256BitsLongForHS256AlgorithmToWork\";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days
    private static final long SHORT_EXPIRATION = 1000; // 1 second (for testing expiration)

    @BeforeEach
    void setUp() {
        // Create new instance for each test
        tokenProvider = new JwtTokenProvider();
        
        // Set private fields using ReflectionTestUtils
        ReflectionTestUtils.setField(tokenProvider, \"jwtSecret\", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(tokenProvider, \"accessTokenExpiration\", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(tokenProvider, \"refreshTokenExpiration\", REFRESH_TOKEN_EXPIRATION);
        
        // Initialize test data
        testUserId = UUID.randomUUID();
        testUsername = \"testuser\";
        testGithubId = 12345678L;
    }

    // ============================================
    // ACCESS TOKEN GENERATION TESTS
    // ============================================

    @Test
    @DisplayName(\"Should generate valid access token\")
    void shouldGenerateValidAccessToken() {
        // When
        String token = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // Then
        assertNotNull(token, \"Token should not be null\");
        assertTrue(token.length() > 0, \"Token should not be empty\");
        assertTrue(token.split(\"\\\\.\").length == 3, \"JWT should have 3 parts (header.payload.signature)\");
    }

    @Test
    @DisplayName(\"Should include correct user ID in access token\")
    void shouldIncludeUserIdInAccessToken() {
        // When
        String token = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // Then
        UUID extractedUserId = tokenProvider.getUserIdFromToken(token);
        assertEquals(testUserId, extractedUserId, \"Extracted user ID should match\");
    }

    @Test
    @DisplayName(\"Should include correct username in access token\")
    void shouldIncludeUsernameInAccessToken() {
        // When
        String token = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // Then
        String extractedUsername = tokenProvider.getUsernameFromToken(token);
        assertEquals(testUsername, extractedUsername, \"Extracted username should match\");
    }

    @Test
    @DisplayName(\"Should include correct GitHub ID in access token\")
    void shouldIncludeGithubIdInAccessToken() {
        // When
        String token = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // Then
        Long extractedGithubId = tokenProvider.getGithubIdFromToken(token);
        assertEquals(testGithubId, extractedGithubId, \"Extracted GitHub ID should match\");
    }

    // ============================================
    // REFRESH TOKEN GENERATION TESTS
    // ============================================

    @Test
    @DisplayName(\"Should generate valid refresh token\")
    void shouldGenerateValidRefreshToken() {
        // When
        String token = tokenProvider.generateRefreshToken(testUserId);

        // Then
        assertNotNull(token, \"Refresh token should not be null\");
        assertTrue(token.length() > 0, \"Refresh token should not be empty\");
        assertTrue(token.split(\"\\\\.\").length == 3, \"JWT should have 3 parts\");
    }

    @Test
    @DisplayName(\"Should include correct user ID in refresh token\")
    void shouldIncludeUserIdInRefreshToken() {
        // When
        String token = tokenProvider.generateRefreshToken(testUserId);

        // Then
        UUID extractedUserId = tokenProvider.getUserIdFromToken(token);
        assertEquals(testUserId, extractedUserId, \"Extracted user ID should match\");
    }

    @Test
    @DisplayName(\"Should identify refresh token correctly\")
    void shouldIdentifyRefreshToken() {
        // When
        String refreshToken = tokenProvider.generateRefreshToken(testUserId);
        String accessToken = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // Then
        assertTrue(tokenProvider.isRefreshToken(refreshToken), \"Should identify refresh token\");
        assertFalse(tokenProvider.isRefreshToken(accessToken), \"Should not identify access token as refresh token\");
    }

    // ============================================
    // TOKEN VALIDATION TESTS
    // ============================================

    @Test
    @DisplayName(\"Should validate correct access token\")
    void shouldValidateCorrectAccessToken() {
        // Given
        String token = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // When
        boolean isValid = tokenProvider.validateToken(token);

        // Then
        assertTrue(isValid, \"Valid token should be validated\");
    }

    @Test
    @DisplayName(\"Should validate correct refresh token\")
    void shouldValidateCorrectRefreshToken() {
        // Given
        String token = tokenProvider.generateRefreshToken(testUserId);

        // When
        boolean isValid = tokenProvider.validateToken(token);

        // Then
        assertTrue(isValid, \"Valid refresh token should be validated\");
    }

    @Test
    @DisplayName(\"Should reject malformed token\")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = \"not.a.valid.jwt.token\";

        // When
        boolean isValid = tokenProvider.validateToken(malformedToken);

        // Then
        assertFalse(isValid, \"Malformed token should be rejected\");
    }

    @Test
    @DisplayName(\"Should reject empty token\")
    void shouldRejectEmptyToken() {
        // When
        boolean isValid = tokenProvider.validateToken(\"\");

        // Then
        assertFalse(isValid, \"Empty token should be rejected\");
    }

    @Test
    @DisplayName(\"Should reject null token\")
    void shouldRejectNullToken() {
        // When/Then
        assertFalse(tokenProvider.validateToken(null), \"Null token should be rejected\");
    }

    @Test
    @DisplayName(\"Should reject tampered token\")
    void shouldRejectTamperedToken() {
        // Given
        String validToken = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + \"XXXXX\";

        // When
        boolean isValid = tokenProvider.validateToken(tamperedToken);

        // Then
        assertFalse(isValid, \"Tampered token should be rejected\");
    }

    // ============================================
    // TOKEN EXPIRATION TESTS
    // ============================================

    @Test
    @DisplayName(\"Should detect expired token\")
    void shouldDetectExpiredToken() throws InterruptedException {
        // Given: Create token provider with very short expiration
        JwtTokenProvider shortExpiryProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpiryProvider, \"jwtSecret\", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(shortExpiryProvider, \"accessTokenExpiration\", SHORT_EXPIRATION);
        
        String token = shortExpiryProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // When: Wait for token to expire
        Thread.sleep(1500); // Wait 1.5 seconds

        // Then
        assertTrue(shortExpiryProvider.isTokenExpired(token), \"Token should be expired\");
        assertFalse(shortExpiryProvider.validateToken(token), \"Expired token should not validate\");
    }

    @Test
    @DisplayName(\"Should not detect non-expired token as expired\")
    void shouldNotDetectNonExpiredTokenAsExpired() {
        // Given
        String token = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // When
        boolean isExpired = tokenProvider.isTokenExpired(token);

        // Then
        assertFalse(isExpired, \"Fresh token should not be expired\");
    }

    // ============================================
    // EDGE CASE TESTS
    // ============================================

    @Test
    @DisplayName(\"Should handle token with different secret\")
    void shouldRejectTokenWithDifferentSecret() {
        // Given: Create token with different secret
        JwtTokenProvider differentSecretProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(differentSecretProvider, \"jwtSecret\", \"differentSecretKey256BitsLongForHS256AlgorithmToWorkProperly\");
        ReflectionTestUtils.setField(differentSecretProvider, \"accessTokenExpiration\", ACCESS_TOKEN_EXPIRATION);
        
        String tokenWithDifferentSecret = differentSecretProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // When: Try to validate with original provider
        boolean isValid = tokenProvider.validateToken(tokenWithDifferentSecret);

        // Then
        assertFalse(isValid, \"Token signed with different secret should be rejected\");
    }

    @Test
    @DisplayName(\"Should generate different tokens for different users\")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Given
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        // When
        String token1 = tokenProvider.generateAccessToken(user1Id, \"user1\", 111L);
        String token2 = tokenProvider.generateAccessToken(user2Id, \"user2\", 222L);

        // Then
        assertNotEquals(token1, token2, \"Tokens for different users should be different\");
    }

    @Test
    @DisplayName(\"Should generate different tokens on each call\")
    void shouldGenerateDifferentTokensOnEachCall() throws InterruptedException {
        // When: Generate two tokens for same user with small time difference
        String token1 = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);
        Thread.sleep(100); // Small delay to ensure different timestamps
        String token2 = tokenProvider.generateAccessToken(testUserId, testUsername, testGithubId);

        // Then
        assertNotEquals(token1, token2, \"Tokens should be different due to different issue times\");
    }
}
