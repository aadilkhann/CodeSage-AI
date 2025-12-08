package com.codesage.service;

import com.codesage.dto.LoginResponse;
import com.codesage.dto.UserDTO;
import com.codesage.model.User;
import com.codesage.repository.UserRepository;
import com.codesage.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AuthService - Authentication business logic
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Service Layer Responsibilities:
 * - Business logic (not in controller)
 * - Transaction management
 * - Coordinate between repositories and other services
 * - Convert entities to DTOs
 * 
 * 2. Why separate service from controller?
 * - Controller: HTTP concerns (request/response, status codes)
 * - Service: Business logic (validation, data transformation)
 * - Testability: Can test service without HTTP layer
 * - Reusability: Service can be used by multiple controllers
 * 
 * 3. Token Refresh Strategy:
 * - Validate refresh token
 * - Load user from database (ensure still exists)
 * - Generate new access token
 * - Optionally rotate refresh token (more secure)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * Refresh access token using refresh token
     * 
     * THOUGHT PROCESS:
     * 
     * Flow:
     * 1. Validate refresh token (signature, expiration)
     * 2. Check it's actually a refresh token (not access token)
     * 3. Extract user ID from token
     * 4. Load user from database (ensure user still exists)
     * 5. Generate new access token
     * 6. Return new tokens
     * 
     * Security:
     * - Refresh token must be valid
     * - User must still exist in database
     * - Can't use access token to refresh (type check)
     * 
     * @param refreshToken Refresh token from client
     * @return LoginResponse with new access token
     * @throws RuntimeException if token invalid or user not found
     */
    @Transactional(readOnly = true)
    public LoginResponse refreshAccessToken(String refreshToken) {
        // Step 1: Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            log.error("Invalid refresh token");
            throw new RuntimeException("Invalid refresh token");
        }

        // Step 2: Check it's a refresh token (not access token)
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            log.error("Access token provided instead of refresh token");
            throw new RuntimeException("Invalid token type");
        }

        // Step 3: Extract user ID
        UUID userId = tokenProvider.getUserIdFromToken(refreshToken);

        // Step 4: Load user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for refresh token: {}", userId);
                    return new RuntimeException("User not found");
                });

        log.info("Refreshing access token for user: {}", user.getUsername());

        // Step 5: Generate new access token
        String newAccessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getGithubId());

        // Step 6: Return response
        // Note: We're returning the same refresh token (not rotating)
        // For better security, could generate new refresh token here
        return new LoginResponse(
                newAccessToken,
                refreshToken, // Same refresh token
                accessTokenExpiration / 1000, // Convert ms to seconds
                convertToDTO(user));
    }

    /**
     * Get current authenticated user info
     * 
     * THOUGHT PROCESS:
     * - Controller extracts user ID from JWT
     * - Service loads full user details from database
     * - Convert to DTO (never expose entity directly)
     * 
     * @param userId User's UUID from JWT
     * @return UserDTO
     * @throws RuntimeException if user not found
     */
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new RuntimeException("User not found");
                });

        return convertToDTO(user);
    }

    /**
     * Convert User entity to UserDTO
     * 
     * THOUGHT PROCESS:
     * - Centralize conversion logic
     * - Easy to maintain
     * - Could use MapStruct or ModelMapper for complex mappings
     * - For now, manual mapping is clear and simple
     * 
     * @param user User entity
     * @return UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setGithubId(user.getGithubId());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
