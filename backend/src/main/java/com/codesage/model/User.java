package com.codesage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Entity - Represents authenticated users from GitHub OAuth
 * 
 * Design Decisions:
 * 1. UUID as primary key: Better for distributed systems, prevents enumeration
 * attacks
 * 2. githubId as unique business key: One GitHub account = one user in our
 * system
 * 3. accessTokenHash: We NEVER store plain text tokens, always hashed for
 * security
 * 4. @EntityListeners: Automatically manage created_at and updated_at
 * timestamps
 * 5. Lombok annotations: Reduce boilerplate (getters, setters, constructors)
 * 
 * Why these fields?
 * - githubId: GitHub's unique identifier (BIGINT because GitHub IDs are large
 * numbers)
 * - username: Display name for UI
 * - email: Contact and notifications (nullable because not all GitHub users
 * have public email)
 * - avatarUrl: Profile picture for UI
 * - accessTokenHash: Hashed OAuth token for GitHub API calls
 */
@Entity
@Table(name = "users")
@Data // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor // Lombok: generates all-args constructor
@EntityListeners(AuditingEntityListener.class) // Enable automatic timestamp management
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * GitHub's unique user ID
     * Why UNIQUE constraint? One GitHub account should map to one user in our
     * system
     */
    @Column(name = "github_id", unique = true, nullable = false)
    private Long githubId;

    /**
     * GitHub username (e.g., "john_doe")
     * Used for display in UI
     */
    @Column(name = "username", nullable = false)
    private String username;

    /**
     * User's email from GitHub
     * Nullable because not all GitHub users have public emails
     */
    @Column(name = "email")
    private String email;

    /**
     * GitHub avatar URL for profile picture
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Hashed OAuth access token
     * SECURITY: Never store plain text tokens!
     * We hash it using BCrypt before storing
     */
    @Column(name = "access_token_hash", length = 500)
    private String accessTokenHash;

    /**
     * When this user was created
     * Automatically set by @CreatedDate annotation
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this user was last updated
     * Automatically updated by @LastModifiedDate annotation
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
