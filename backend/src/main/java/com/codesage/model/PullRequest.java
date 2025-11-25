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
 * PullRequest Entity - Represents GitHub pull requests being analyzed
 * 
 * Key Design Decisions:
 * 1. Composite unique constraint: (repository_id, pr_number)
 * - PR #42 can exist in multiple repos, but only once per repo
 * 2. Denormalized data: Cache title, author, etc. to avoid GitHub API calls
 * 3. Status tracking: open, closed, merged
 * 4. Stats fields: files_changed, additions, deletions for quick display
 * 
 * Relationship:
 * - ManyToOne with Repository: Many PRs belong to one repository
 * - OneToMany with Analysis: One PR can have multiple analyses (when updated)
 */
@Entity
@Table(name = "pull_requests", uniqueConstraints = @UniqueConstraint(name = "uq_pr_repo_number", columnNames = {
        "repository_id", "pr_number" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PullRequest {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    /**
     * The repository this PR belongs to
     * LAZY loading: Don't fetch repository unless needed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    /**
     * PR number (e.g., #42)
     * Combined with repository_id forms unique constraint
     */
    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * GitHub username of PR author
     * Denormalized for performance (avoid GitHub API call)
     */
    @Column(name = "author")
    private String author;

    @Column(name = "base_branch")
    private String baseBranch;

    @Column(name = "head_branch")
    private String headBranch;

    /**
     * PR status: open, closed, merged
     * Helps filter active PRs
     */
    @Column(name = "status", length = 50)
    private String status = "open";

    /**
     * Direct link to PR on GitHub
     * Format: https://github.com/owner/repo/pull/42
     */
    @Column(name = "github_url", length = 500)
    private String githubUrl;

    /**
     * Quick stats for display (avoid parsing diff every time)
     */
    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Helper method to check if PR is still open
     */
    public boolean isOpen() {
        return "open".equals(status);
    }

    /**
     * Helper method to check if PR is merged
     */
    public boolean isMerged() {
        return "merged".equals(status);
    }
}
