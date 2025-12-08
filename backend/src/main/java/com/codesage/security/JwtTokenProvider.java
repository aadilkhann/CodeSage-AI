package com.codesage.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JwtTokenProvider - Generate and validate JWT tokens
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why JWT?
 * - Stateless: No server-side session storage needed
 * - Scalable: Works across multiple backend instances
 * - Self-contained: Token includes all user info
 * - Standard: Industry-standard authentication method
 * 
 * 2. Token Types:
 * - Access Token: Short-lived (1 hour), used for API requests
 * - Refresh Token: Long-lived (7 days), used to get new access tokens
 * 
 * 3. Security Measures:
 * - Strong secret key (256-bit minimum)
 * - Token expiration
 * - Signature validation
 * - Claims validation
 * 
 * 4. Token Structure:
 * Header: {"alg": "HS256", "typ": "JWT"}
 * Payload: {"sub": "user-id", "username": "john", "exp": 1234567890}
 * Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * JWT secret key from environment variable
     * SECURITY: Must be at least 256 bits (32 bytes)
     * Generate with: openssl rand -hex 32
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Access token expiration: 1 hour (in milliseconds)
     * Why 1 hour? Balance between security and user experience
     * - Too short: Users constantly re-authenticate (bad UX)
     * - Too long: Stolen tokens valid longer (security risk)
     */
    @Value("${app.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    /**
     * Refresh token expiration: 7 days (in milliseconds)
     * Why 7 days? Users don't need to login every day
     * - Stolen refresh token has limited window
     * - Can be revoked if suspicious activity detected
     */
    @Value("${app.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    /**
     * Generate access token for authenticated user
     * 
     * THOUGHT PROCESS:
     * - Include minimal user info in token (reduce size)
     * - Don't include sensitive data (token can be decoded)
     * - Include roles for authorization
     * 
     * @param userId   User's UUID
     * @param username GitHub username
     * @param githubId GitHub user ID
     * @return JWT access token
     */
    public String generateAccessToken(UUID userId, String username, Long githubId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        // Build claims (payload)
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("githubId", githubId);
        claims.put("type", "access");

        // Generate token
        return Jwts.builder()
                .setSubject(userId.toString()) // User ID as subject
                .setClaims(claims) // Additional user info
                .setIssuedAt(now) // When token was created
                .setExpiration(expiryDate) // When token expires
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Sign with secret
                .compact();
    }

    /**
     * Generate refresh token
     * 
     * THOUGHT PROCESS:
     * - Minimal claims (just user ID and type)
     * - Longer expiration
     * - Used only for getting new access tokens
     * 
     * @param userId User's UUID
     * @return JWT refresh token
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return Jwts.builder()
                .setSubject(userId.toString())
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Get user ID from token
     * 
     * THOUGHT PROCESS:
     * - Subject (sub) claim contains user ID
     * - Parse as UUID for type safety
     * 
     * @param token JWT token
     * @return User's UUID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Get username from token
     * 
     * @param token JWT token
     * @return Username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Get GitHub ID from token
     * 
     * @param token JWT token
     * @return GitHub user ID
     */
    public Long getGithubIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("githubId", Long.class);
    }

    /**
     * Validate token
     * 
     * THOUGHT PROCESS:
     * Validation checks:
     * 1. Signature is valid (token not tampered with)
     * 2. Token not expired
     * 3. Token structure is correct
     * 
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired
     * 
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * Check if token is a refresh token
     * 
     * THOUGHT PROCESS:
     * - Refresh tokens should only be used for token refresh endpoint
     * - Access tokens should be used for API requests
     * - Prevents misuse of tokens
     * 
     * @param token JWT token
     * @return true if refresh token
     */
    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        String type = claims.get("type", String.class);
        return "refresh".equals(type);
    }

    /**
     * Parse and validate token, extract claims
     * 
     * THOUGHT PROCESS:
     * - Central method for token parsing
     * - Validates signature automatically
     * - Throws exceptions if invalid
     * 
     * @param token JWT token
     * @return Claims from token
     * @throws JwtException if token is invalid
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get signing key from secret
     * 
     * THOUGHT PROCESS:
     * - Convert string secret to SecretKey
     * - Use HMAC-SHA256 algorithm
     * - Key must be at least 256 bits for HS256
     * 
     * @return SecretKey for signing
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
