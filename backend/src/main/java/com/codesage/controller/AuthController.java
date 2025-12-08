package com.codesage.controller;

import com.codesage.dto.LoginResponse;
import com.codesage.dto.TokenRefreshRequest;
import com.codesage.dto.UserDTO;
import com.codesage.security.JwtTokenProvider;
import com.codesage.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AuthController - REST API endpoints for authentication
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Controller Responsibilities:
 * - Handle HTTP requests/responses
 * - Validate input
 * - Call service layer
 * - Return appropriate HTTP status codes
 * - Handle exceptions
 * 
 * 2. Endpoint Design:
 * - POST /api/v1/auth/refresh - Refresh access token
 * - POST /api/v1/auth/logout - Logout user (client-side, just info)
 * - GET /api/v1/auth/me - Get current user info
 * 
 * 3. Why these endpoints?
 * - /refresh: Access tokens expire, need way to get new one
 * - /logout: Clear tokens on client, inform server (for future: token
 * blacklist)
 * - /me: Get current user info (for profile display, etc.)
 * 
 * 4. Authentication:
 * - /refresh: Public (uses refresh token in body)
 * - /logout: Protected (requires valid access token)
 * - /me: Protected (requires valid access token)
 * 
 * Note: GitHub OAuth login is handled by Spring Security automatically
 * (configured in SecurityConfig)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    /**
     * Refresh access token
     * 
     * THOUGHT PROCESS:
     * 
     * Endpoint: POST /api/v1/auth/refresh
     * Request Body: {"refreshToken": "eyJhbGc..."}
     * Response: {"accessToken": "...", "refreshToken": "...", ...}
     * 
     * Flow:
     * 1. Client's access token expires
     * 2. Client sends refresh token to this endpoint
     * 3. Server validates refresh token
     * 4. Server generates new access token
     * 5. Client updates stored access token
     * 6. Client retries original request
     * 
     * Security:
     * - This endpoint is PUBLIC (no access token required)
     * - Refresh token itself provides authentication
     * - Validate refresh token thoroughly in service layer
     * 
     * @param request Token refresh request
     * @return LoginResponse with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        log.info("Token refresh request received");

        try {
            LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
            log.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Logout user
     * 
     * THOUGHT PROCESS:
     * 
     * Endpoint: POST /api/v1/auth/logout
     * Response: {"message": "Logged out successfully"}
     * 
     * JWT Logout Strategy:
     * - JWTs are stateless (server doesn't track them)
     * - Can't "invalidate" a JWT on server side (without token blacklist)
     * - Client-side logout: Delete tokens from localStorage
     * - Server-side: Just log the event (for analytics)
     * 
     * Future Enhancement:
     * - Implement token blacklist (Redis)
     * - Add token to blacklist on logout
     * - Check blacklist in JwtAuthenticationFilter
     * 
     * For now:
     * - Simple endpoint that returns success
     * - Client deletes tokens
     * - Token will expire naturally
     * 
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Get current user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String userId = authentication.getName();
            log.info("User logged out: {}", userId);
        }

        // For JWT, logout is primarily client-side
        // Client will delete tokens from localStorage
        return ResponseEntity.ok().body(new LogoutResponse("Logged out successfully"));
    }

    /**
     * Get current authenticated user
     * 
     * THOUGHT PROCESS:
     * 
     * Endpoint: GET /api/v1/auth/me
     * Response: {"id": "...", "username": "...", ...}
     * 
     * Use Cases:
     * - Frontend needs user info on page load
     * - Display user profile in header
     * - Check if user is still authenticated
     * 
     * How it works:
     * 1. Client sends request with access token in header
     * 2. JwtAuthenticationFilter validates token
     * 3. Filter extracts user ID and sets in SecurityContext
     * 4. Controller gets user ID from SecurityContext
     * 5. Service loads full user details from database
     * 6. Return user info as DTO
     * 
     * @return Current user info
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        // Get user ID from security context (set by JwtAuthenticationFilter)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated request to /me endpoint");
            return ResponseEntity.status(401).build();
        }

        // Authentication name is user ID (set in CustomUserDetailsService)
        String userIdStr = authentication.getName();
        UUID userId = UUID.fromString(userIdStr);

        log.debug("Getting current user info for: {}", userId);

        UserDTO user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Simple response class for logout endpoint
     */
    private record LogoutResponse(String message) {
    }
}
