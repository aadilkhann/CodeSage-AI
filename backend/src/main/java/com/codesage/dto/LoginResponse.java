package com.codesage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * LoginResponse - Response after successful authentication
 * 
 * THOUGHT PROCESS:
 * 
 * 1. What does the frontend need after login?
 * - Access token (for API requests)
 * - Refresh token (to get new access tokens)
 * - User info (to display in UI)
 * - Token expiration (to know when to refresh)
 * 
 * 2. Why separate DTO from User entity?
 * - Security: Don't expose internal fields (accessTokenHash, etc.)
 * - Flexibility: Can add/remove fields without changing entity
 * - Clean API: Only send what frontend needs
 * 
 * 3. Token expiration:
 * - Frontend needs to know when to refresh token
 * - Send as "expiresIn" (seconds from now)
 * - Frontend calculates absolute expiry time
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT access token
     * Frontend includes this in Authorization header for API requests
     * Format: "Bearer <accessToken>"
     */
    private String accessToken;

    /**
     * JWT refresh token
     * Used to get new access token when current one expires
     * Stored in localStorage, sent to /api/v1/auth/refresh
     */
    private String refreshToken;

    /**
     * Token type (always "Bearer" for JWT)
     * Tells frontend how to use the token
     */
    private String tokenType = "Bearer";

    /**
     * Access token expiration in seconds
     * Example: 3600 means token expires in 1 hour
     * Frontend calculates: expiryTime = now + expiresIn
     */
    private long expiresIn;

    /**
     * Authenticated user information
     */
    private UserDTO user;

    /**
     * Convenience constructor without tokenType (defaults to "Bearer")
     */
    public LoginResponse(String accessToken, String refreshToken, long expiresIn, UserDTO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
