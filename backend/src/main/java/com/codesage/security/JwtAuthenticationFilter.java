package com.codesage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JwtAuthenticationFilter - Intercept HTTP requests and validate JWT tokens
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why a Filter?
 * - Filters run before controllers
 * - Can intercept ALL requests
 * - Perfect for authentication (check before processing request)
 * 
 * 2. OncePerRequestFilter:
 * - Guarantees filter runs exactly once per request
 * - Prevents duplicate authentication checks
 * 
 * 3. Filter Flow:
 * Request → Extract JWT → Validate → Set Authentication → Continue
 * 
 * 4. Security Context:
 * - Spring Security uses SecurityContextHolder
 * - Stores authentication for current request
 * - Controllers can access via @AuthenticationPrincipal
 * 
 * 5. What happens if no token?
 * - Request continues (might be public endpoint)
 * - SecurityConfig determines if endpoint requires auth
 * - If required, Spring Security returns 401 Unauthorized
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Main filter logic - runs for every HTTP request
     * 
     * THOUGHT PROCESS:
     * 1. Extract JWT from Authorization header
     * 2. Validate token
     * 3. Get user ID from token
     * 4. Load user details from database
     * 5. Create authentication object
     * 6. Set in SecurityContext
     * 7. Continue filter chain
     * 
     * @param request     HTTP request
     * @param response    HTTP response
     * @param filterChain Filter chain to continue
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Step 1: Extract JWT from request
            String jwt = extractJwtFromRequest(request);

            // Step 2: If JWT exists and is valid
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                // Step 3: Check if it's an access token (not refresh token)
                if (!tokenProvider.isRefreshToken(jwt)) {

                    // Step 4: Get user ID from token
                    UUID userId = tokenProvider.getUserIdFromToken(jwt);

                    // Step 5: Load user details from database
                    // Why load from DB? Token might be valid but user could be deleted/disabled
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());

                    // Step 6: Create authentication object
                    // This tells Spring Security: "This user is authenticated"
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // No credentials needed (already authenticated via JWT)
                            userDetails.getAuthorities() // User's roles/permissions
                    );

                    // Step 7: Add request details (IP address, session ID, etc.)
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // Step 8: Set authentication in SecurityContext
                    // Now Spring Security knows this request is authenticated
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication for user: {}", userId);
                } else {
                    log.warn("Refresh token used for API request - rejecting");
                }
            }
        } catch (Exception ex) {
            // Log error but don't stop request
            // Let SecurityConfig handle unauthorized access
            log.error("Could not set user authentication in security context", ex);
        }

        // Step 9: Continue to next filter or controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * 
     * THOUGHT PROCESS:
     * - Standard format: "Authorization: Bearer <token>"
     * - "Bearer" is the authentication scheme
     * - Extract everything after "Bearer "
     * 
     * Example:
     * Header: "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * Returns: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * 
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Check if Authorization header exists and starts with "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Extract token (remove "Bearer " prefix)
            return bearerToken.substring(7);
        }

        return null;
    }
}
