package com.codesage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TokenRefreshRequest - Request to refresh access token
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why separate endpoint for token refresh?
 * - Access tokens expire quickly (1 hour)
 * - Don't want user to re-login every hour
 * - Refresh token lasts longer (7 days)
 * - Exchange refresh token for new access token
 * 
 * 2. Security considerations:
 * - Validate refresh token is not expired
 * - Validate refresh token is actually a refresh token (not access token)
 * - Generate new access token with same user info
 * - Optionally rotate refresh token (generate new one)
 * 
 * 3. Flow:
 * - Frontend detects access token expired (401 response)
 * - Frontend sends refresh token to /api/v1/auth/refresh
 * - Backend validates refresh token
 * - Backend generates new access token
 * - Frontend updates stored access token
 * - Frontend retries original request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

    /**
     * Refresh token from initial login
     * Must be valid and not expired
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
