package com.codesage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CodePattern Entity - Stores learned patterns from approved PRs for RAG
 * 
 * Key Design Decisions:
 * 1. Store code snippets that were approved in merged PRs
 * 2. embedding_id: Reference to vector in Chroma DB for similarity search
 * 3. frequency: Track how often pattern appears (popular patterns = more
 * important)
 * 4. Used in RAG pipeline to find similar code patterns
 * 
 * Example Usage:
 * When analyzing new code, we:
 * 1. Generate embedding for new code
 * 2. Search Chroma for similar embeddings
 * 3. Retrieve matching CodePattern records
 * 4. Use as context for Gemini to generate suggestions
 */
@Entity
@Table(name = "code_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodePattern {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    /**
     * Type of pattern
     * Examples: naming_convention, error_handling, class_structure,
     * method_signature, import_organization
     */
    @Column(name = "pattern_type", length = 100)
    private String patternType;

    /**
     * Programming language
     * Helps apply language-specific patterns
     */
    @Column(name = "language", length = 50)
    private String language;

    /**
     * The actual code snippet
     * Example: "public class UserService implements IUserService {"
     */
    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    /**
     * Context/description of when this pattern is used
     * Example: "Service classes implement interface and use dependency injection"
     */
    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    /**
     * Reference to vector embedding in Chroma DB
     * Used to retrieve the embedding for similarity search
     */
    @Column(name = "embedding_id")
    private String embeddingId;

    /**
     * How many times we've seen this pattern
     * Higher frequency = more established pattern in this repo
     */
    @Column(name = "frequency")
    private Integer frequency = 1;

    /**
     * When we last saw this pattern
     * Helps identify outdated patterns
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Increment frequency when pattern is seen again
     */
    public void incrementFrequency() {
        this.frequency++;
        this.lastSeen = LocalDateTime.now();
    }
}
