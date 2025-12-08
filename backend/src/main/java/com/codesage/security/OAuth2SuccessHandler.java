package com.codesage.security;

import com.codesage.model.User;
import com.codesage.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2SuccessHandler - Handle successful GitHub OAuth authentication
 * 
 * THOUGHT PROCESS:
 * 
 * 1. When is this called?
 * - After user successfully authenticates with GitHub
 * - GitHub redirects to our callback URL
 * - Spring Security processes OAuth response
 * - This handler is called with user info from GitHub
 * 
 * 2. What do we do?
 * - Extract user info from GitHub (username, email, avatar, etc.)
 * - Create or update user in our database
 * - Generate JWT tokens (access + refresh)
 * - Redirect to frontend with tokens
 * 
 * 3. Why redirect to frontend?
 * - Frontend needs tokens to make API requests
 * - Pass tokens as URL parameters (temporary, frontend stores in localStorage)
 * - Alternative: Use cookies (more secure but complex)
 * 
 * 4. Security considerations:
 * - Hash GitHub access token before storing
 * - Don't log sensitive data
 * - Use HTTPS in production (tokens in URL visible in logs)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * Frontend URL to redirect after successful login
     * Example: http://localhost:5173 (development)
     * https://your-domain.com (production)
     */
    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Handle successful OAuth authentication
     * 
     * FLOW:
     * 1. User clicks "Login with GitHub" on frontend
     * 2. Frontend redirects to GitHub OAuth
     * 3. User authorizes on GitHub
     * 4. GitHub redirects to our backend callback
     * 5. Spring Security processes OAuth response
     * 6. THIS METHOD IS CALLED ← We are here!
     * 7. We create/update user
     * 8. We generate JWT tokens
     * 9. We redirect to frontend with tokens
     * 10. Frontend stores tokens and user is logged in
     * 
     * @param request        HTTP request
     * @param response       HTTP response
     * @param authentication Authentication object with GitHub user info
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        // Step 1: Extract GitHub user info
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // GitHub provides these attributes:
        // - id: GitHub user ID (Long)
        // - login: GitHub username (String)
        // - email: User's email (String, might be null)
        // - avatar_url: Profile picture URL (String)
        // - name: Full name (String, might be null)

        Long githubId = oAuth2User.getAttribute("id");
        String username = oAuth2User.getAttribute("login");
        String email = oAuth2User.getAttribute("email");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");

        log.info("OAuth2 success for GitHub user: {} (ID: {})", username, githubId);

        // Step 2: Create or update user in database
        User user = createOrUpdateUser(githubId, username, email, avatarUrl);

        // Step 3: Generate JWT tokens
        String accessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getGithubId());

        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        log.debug("Generated tokens for user: {}", user.getId());

        // Step 4: Build redirect URL with tokens
        // Format: http://localhost:5173/auth/callback?token=xxx&refresh=yyy
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refresh", refreshToken)
                .build()
                .toUriString();

        // Step 5: Redirect to frontend
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Create new user or update existing user
     * 
     * THOUGHT PROCESS:
     * - Check if user exists by GitHub ID
     * - If exists: Update info (username, email, avatar might change)
     * - If not exists: Create new user
     * - Why update? User might change GitHub username/email/avatar
     * 
     * @param githubId  GitHub user ID
     * @param username  GitHub username
     * @param email     User's email
     * @param avatarUrl Profile picture URL
     * @return User entity
     */
    private User createOrUpdateUser(Long githubId, String username, String email, String avatarUrl) {
        return userRepository.findByGithubId(githubId)
                .map(existingUser -> {
                    // Update existing user
                    log.debug("Updating existing user: {}", existingUser.getId());
                    existingUser.setUsername(username);
                    existingUser.setEmail(email);
                    existingUser.setAvatarUrl(avatarUrl);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // Create new user
                    log.info("Creating new user for GitHub ID: {}", githubId);
                    User newUser = new User();
                    newUser.setGithubId(githubId);
                    newUser.setUsername(username);
                    newUser.setEmail(email);
                    newUser.setAvatarUrl(avatarUrl);
                    // Note: accessTokenHash will be set later when we need to call GitHub API
                    return userRepository.save(newUser);
                });
    }
}
