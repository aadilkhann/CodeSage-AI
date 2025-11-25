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
 * AnalyticsEvent Entity - Track events for analytics and metrics
 * 
 * Key Design Decisions:
 * 1. Generic event tracking for flexibility
 * 2. metadata JSONB: Store event-specific data
 * 3. SET NULL on user/repo delete: Keep analytics even if entities deleted
 * 4. Used for dashboard metrics, trends, and business intelligence
 * 
 * Example Events:
 * - pr_analyzed: {"duration_ms": 95000, "suggestions_count": 5}
 * - suggestion_accepted: {"category": "naming", "confidence": 85}
 * - suggestion_rejected: {"category": "performance", "reason": "not
 * applicable"}
 * - repository_added: {"language": "Java"}
 */
@Entity
@Table(name = "analytics_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    /**
     * Event type
     * Examples: pr_analyzed, suggestion_accepted, suggestion_rejected,
     * repository_added, analysis_failed
     */
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    /**
     * User who triggered the event (nullable, SET NULL on delete)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Repository related to the event (nullable, SET NULL on delete)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private Repository repository;

    /**
     * Event-specific metadata
     * 
     * Examples:
     * pr_analyzed: {
     * "pr_number": 42,
     * "duration_ms": 95000,
     * "suggestions_count": 5,
     * "files_analyzed": 10
     * }
     * 
     * suggestion_accepted: {
     * "suggestion_id": "uuid",
     * "category": "naming",
     * "severity": "minor",
     * "confidence": 85
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
