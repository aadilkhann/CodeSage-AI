package com.codesage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Suggestion Entity - AI-generated code review suggestions
 * 
 * Key Design Decisions:
 * 1. Many-to-One with Analysis: One analysis produces many suggestions
 * 2. Category & Severity: Classify and prioritize suggestions
 * 3. Confidence Score: AI's confidence (0-100), filter low-confidence
 * suggestions
 * 4. Status tracking: pending → accepted/rejected/ignored
 * 5. User feedback: Learn from user decisions (future ML improvement)
 * 
 * Categories: naming, architecture, performance, security, testing,
 * error_handling, etc.
 * Severity: critical, moderate, minor
 * Status: pending, accepted, rejected, ignored
 */
@Entity
@Table(name = "suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Suggestion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    /**
     * File path relative to repository root
     * Example: "src/main/java/com/example/Service.java"
     */
    @Column(name = "file_path", length = 500, nullable = false)
    private String filePath;

    /**
     * Line number where issue starts
     */
    @Column(name = "line_number")
    private Integer lineNumber;

    /**
     * Line number where issue ends (for multi-line suggestions)
     * If null, suggestion applies to single line
     */
    @Column(name = "line_end")
    private Integer lineEnd;

    /**
     * Suggestion category
     * Examples: naming, architecture, performance, security, testing
     * Used for filtering and analytics
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Severity level: critical, moderate, minor
     * Helps prioritize which suggestions to address first
     */
    @Column(name = "severity", length = 50)
    private String severity;

    /**
     * Short message describing the issue
     * Example: "Variable name 'x' is not descriptive"
     */
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * Detailed explanation with context
     * Example: "Based on similar code in this repository, variable names
     * should be descriptive. In UserService.java:42, you used 'userCount'
     * for a similar purpose."
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    /**
     * Suggested code fix
     * Example: "int userCount = users.size();"
     */
    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    /**
     * AI's confidence in this suggestion (0.00 to 100.00)
     * Higher confidence = more likely to be valid
     * Can filter suggestions below threshold (e.g., only show if > 70)
     */
    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    /**
     * Suggestion status:
     * - pending: Awaiting user action
     * - accepted: User accepted the suggestion
     * - rejected: User rejected the suggestion
     * - ignored: User chose to ignore
     */
    @Column(name = "status", length = 50)
    private String status = "pending";

    /**
     * User's feedback on why they accepted/rejected
     * Used to improve future suggestions
     * Example: "Not applicable in this context" or "Good catch!"
     */
    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isAccepted() {
        return "accepted".equals(status);
    }

    public boolean isRejected() {
        return "rejected".equals(status);
    }

    public boolean isCritical() {
        return "critical".equals(severity);
    }

    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore.compareTo(new BigDecimal("70")) >= 0;
    }

    /**
     * Accept this suggestion with optional feedback
     */
    public void accept(String feedback) {
        this.status = "accepted";
        this.userFeedback = feedback;
    }

    /**
     * Reject this suggestion with optional feedback
     */
    public void reject(String feedback) {
        this.status = "rejected";
        this.userFeedback = feedback;
    }
}
