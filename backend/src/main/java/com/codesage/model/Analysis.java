package com.codesage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Analysis Entity - Tracks the analysis process for each PR
 * 
 * Key Design Decisions:
 * 1. One-to-Many with PullRequest: A PR can be analyzed multiple times
 * - When PR is updated, we create a new analysis
 * - Keeps history of all analyses
 * 2. Status lifecycle: pending → processing → completed/failed
 * 3. Performance tracking: duration_ms, files_analyzed
 * 4. Metadata JSONB: Store flexible analysis details
 * Example: {"gemini_tokens": 1500, "patterns_matched": 5, "cache_hits": 3}
 * 
 * Relationship:
 * - ManyToOne with PullRequest: Many analyses for one PR
 * - OneToMany with Suggestion: One analysis produces many suggestions
 */
@Entity
@Table(name = "analyses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    /**
     * The PR being analyzed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    /**
     * Analysis status lifecycle:
     * - pending: Queued for analysis
     * - processing: Currently being analyzed
     * - completed: Successfully completed
     * - failed: Analysis failed (see error_message)
     */
    @Column(name = "status", length = 50)
    private String status = "pending";

    /**
     * When analysis started
     * Used to calculate duration and detect stuck analyses
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * When analysis completed (success or failure)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Analysis duration in milliseconds
     * Performance metric: Track slow analyses
     * Calculated: completedAt - startedAt
     */
    @Column(name = "duration_ms")
    private Integer durationMs;

    /**
     * Number of files analyzed
     * Helps understand analysis scope
     */
    @Column(name = "files_analyzed")
    private Integer filesAnalyzed;

    /**
     * Error message if analysis failed
     * Helps debug failures
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Flexible metadata stored as JSONB
     * 
     * Example metadata:
     * {
     * "gemini_tokens_used": 1500,
     * "gemini_requests": 3,
     * "patterns_matched": 5,
     * "cache_hits": 2,
     * "files_skipped": ["package-lock.json"],
     * "analysis_version": "1.0"
     * }
     * 
     * Why JSONB?
     * - Track API usage (important for rate limiting)
     * - Store analysis-specific details
     * - Add new metrics without schema changes
     */
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Helper method to check if analysis is complete
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * Helper method to check if analysis failed
     */
    public boolean isFailed() {
        return "failed".equals(status);
    }

    /**
     * Helper method to check if analysis is in progress
     */
    public boolean isProcessing() {
        return "processing".equals(status);
    }

    /**
     * Helper method to mark analysis as started
     */
    public void markAsStarted() {
        this.status = "processing";
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Helper method to mark analysis as completed
     */
    public void markAsCompleted() {
        this.status = "completed";
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationMs = (int) java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    /**
     * Helper method to mark analysis as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = "failed";
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        if (this.startedAt != null) {
            this.durationMs = (int) java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }
}
