package com.codesage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Repository Entity - Represents GitHub repositories being monitored
 * 
 * Design Decisions:
 * 1. ManyToOne with User: Each repository belongs to one user (who added it)
 * 2. githubRepoId as unique business key: GitHub's unique repository ID
 * 3. JSONB for settings: Flexible configuration without schema changes
 * Example settings: {"autoAnalyze": true, "minConfidenceScore": 70,
 * "enabledCategories": ["naming", "architecture"]}
 * 4. isActive flag: Soft enable/disable without losing historical data
 * 5. webhookId and webhookSecret: Manage GitHub webhooks
 * 
 * Why these fields?
 * - user: Who added this repository (for access control)
 * - githubRepoId: GitHub's unique ID (different from our UUID)
 * - fullName: "owner/repo-name" format for display
 * - language: Primary programming language (helps with analysis)
 * - webhookId: GitHub's webhook ID (for updating/deleting webhook)
 * - webhookSecret: Verify webhook authenticity
 * - isActive: Pause monitoring without deleting data
 * - settings: Per-repository configuration (JSONB for flexibility)
 */
@Entity
@Table(name = "repositories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Repository {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who added this repository
     * ManyToOne: Many repositories can belong to one user
     * FetchType.LAZY: Don't load user unless explicitly requested (performance)
     * CascadeType: No cascade - we don't want deleting repo to delete user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * GitHub's unique repository ID
     * Why UNIQUE? One GitHub repository should only be added once to our system
     */
    @Column(name = "github_repo_id", unique = true, nullable = false)
    private Long githubRepoId;

    /**
     * Repository full name in "owner/repo-name" format
     * Example: "facebook/react" or "spring-projects/spring-boot"
     */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /**
     * Repository description from GitHub
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Primary programming language
     * Used to apply language-specific analysis rules
     */
    @Column(name = "language", length = 50)
    private String language;

    /**
     * GitHub webhook ID
     * Stored so we can update or delete the webhook later
     */
    @Column(name = "webhook_id")
    private Long webhookId;

    /**
     * Webhook secret for signature verification
     * Each repository has its own secret for security
     */
    @Column(name = "webhook_secret")
    private String webhookSecret;

    /**
     * Is this repository actively being monitored?
     * Soft enable/disable: false means pause monitoring, but keep historical data
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Flexible repository settings stored as JSONB
     * 
     * Example settings:
     * {
     * "autoAnalyze": true, // Automatically analyze new PRs
     * "minConfidenceScore": 70, // Only show suggestions with confidence >= 70
     * "enabledCategories": [ // Which categories to analyze
     * "naming",
     * "architecture",
     * "performance"
     * ],
     * "excludePatterns": [ // Files to exclude from analysis
     * "*.test.js",
     * "migrations/*"
     * ]
     * }
     * 
     * Why JSONB?
     * - Flexible: Add new settings without schema changes
     * - Queryable: PostgreSQL can query inside JSONB
     * - Per-repo customization: Each repo can have different settings
     */
    @Type(JsonBinaryType.class)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Helper method to check if auto-analysis is enabled
     */
    public boolean isAutoAnalyzeEnabled() {
        return settings.getOrDefault("autoAnalyze", true).equals(true);
    }

    /**
     * Helper method to get minimum confidence score
     */
    public int getMinConfidenceScore() {
        return (int) settings.getOrDefault("minConfidenceScore", 70);
    }
}
