# Backend Model Class Diagram

This document contains a detailed class diagram of the backend models for the CodeSage AI project.

## Class Diagram

```mermaid
classDiagram
    class User {
        -UUID id
        -Long githubId
        -String username
        -String email
        -String avatarUrl
        -String accessTokenHash
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class Repository {
        -UUID id
        -Long githubRepoId
        -String fullName
        -String description
        -String language
        -Long webhookId
        -String webhookSecret
        -Boolean isActive
        -Map~String, Object~ settings
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +isAutoAnalyzeEnabled() boolean
        +getMinConfidenceScore() int
    }

    class PullRequest {
        -UUID id
        -Integer prNumber
        -String title
        -String description
        -String author
        -String baseBranch
        -String headBranch
        -String status
        -String githubUrl
        -Integer filesChanged
        -Integer additions
        -Integer deletions
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +isOpen() boolean
        +isMerged() boolean
    }

    class Analysis {
        -UUID id
        -String status
        -LocalDateTime startedAt
        -LocalDateTime completedAt
        -Integer durationMs
        -Integer filesAnalyzed
        -String errorMessage
        -Map~String, Object~ metadata
        -LocalDateTime createdAt
        +isCompleted() boolean
        +isFailed() boolean
        +isProcessing() boolean
        +markAsStarted() void
        +markAsCompleted() void
        +markAsFailed(String) void
    }

    class Suggestion {
        -UUID id
        -String filePath
        -Integer lineNumber
        -Integer lineEnd
        -String category
        -String severity
        -String message
        -String explanation
        -String suggestedFix
        -BigDecimal confidenceScore
        -String status
        -String userFeedback
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +isPending() boolean
        +isAccepted() boolean
        +isRejected() boolean
        +isCritical() boolean
        +isHighConfidence() boolean
        +accept(String) void
        +reject(String) void
    }

    class CodePattern {
        -UUID id
        -String patternType
        -String language
        -String codeSnippet
        -String context
        -String embeddingId
        -Integer frequency
        -LocalDateTime lastSeen
        -LocalDateTime createdAt
        +incrementFrequency() void
    }

    class AnalyticsEvent {
        -UUID id
        -String eventType
        -Map~String, Object~ metadata
        -LocalDateTime createdAt
    }

    %% Relationships
    Repository "0..*" -- "1" User : belongs to
    PullRequest "0..*" -- "1" Repository : belongs to
    Analysis "0..*" -- "1" PullRequest : analyzes
    Suggestion "0..*" -- "1" Analysis : generated from
    CodePattern "0..*" -- "1" Repository : learned from
    AnalyticsEvent "0..*" -- "0..1" User : triggered by
    AnalyticsEvent "0..*" -- "0..1" Repository : related to
```

## Entity Details

### User
Represents authenticated users from GitHub OAuth.
- **Key Fields**: `githubId` (unique), `accessTokenHash` (securely stored)

### Repository
Represents GitHub repositories being monitored.
- **Key Fields**: `githubRepoId` (unique), `settings` (JSONB for flexibility), `isActive`
- **Relationships**: Belongs to a User.

### PullRequest
Represents GitHub pull requests being analyzed.
- **Key Constraints**: Unique combination of (`repository_id`, `pr_number`)
- **Relationships**: Belongs to a Repository.

### Analysis
Tracks the analysis process for a Pull Request.
- **Lifecycle**: pending -> processing -> completed/failed
- **Relationships**: Link between PullRequest and Suggestions.

### Suggestion
AI-generated code review suggestions.
- **Key Fields**: `confidenceScore`, `severity`, `status` (pending/accepted/rejected)
- **Relationships**: Belong to an Analysis.

### CodePattern
Stores learned patterns from approved PRs for RAG (Retrieval-Augmented Generation).
- **Key Fields**: `embeddingId` (for vector search), `codeSnippet`
- **Relationships**: Linked to a Repository.

### AnalyticsEvent
Generic event tracking for analytics and metrics.
- **Key Fields**: `eventType`, `metadata` (JSONB)
- **Relationships**: Can be linked to User and/or Repository (nullable).
