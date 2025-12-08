package com.codesage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserDTO - User data transfer object
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why separate DTO from User entity?
 * - Security: Never expose sensitive fields
 * ❌ Don't send: accessTokenHash, internal IDs
 * ✅ Do send: username, email, avatar
 * - API Stability: Can change entity without breaking API
 * - Clean Response: Only relevant fields for frontend
 * 
 * 2. What does frontend need?
 * - Basic info: username, email, avatar (for display)
 * - GitHub link: to view user's GitHub profile
 * - Account age: when user joined (for analytics)
 * 
 * 3. Conversion from Entity:
 * - Create static factory method: UserDTO.fromEntity(User)
 * - Keeps conversion logic in one place
 * - Easy to maintain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /**
     * User's unique ID
     * Frontend uses this to identify user
     */
    private UUID id;

    /**
     * GitHub username
     * Example: "octocat"
     */
    private String username;

    /**
     * User's email (might be null if not public on GitHub)
     */
    private String email;

    /**
     * GitHub avatar URL
     * Example: "https://avatars.githubusercontent.com/u/123456"
     */
    private String avatarUrl;

    /**
     * GitHub user ID
     * Can be used to construct GitHub profile URL
     */
    private Long githubId;

    /**
     * When user first logged in to our system
     */
    private LocalDateTime createdAt;

    /**
     * Construct GitHub profile URL
     * Example: "https://github.com/octocat"
     */
    public String getGithubProfileUrl() {
        return "https://github.com/" + username;
    }
}
