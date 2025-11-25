-- CodeSage AI - Initial Database Schema
-- Version: V1
-- Description: Create core tables for users, repositories, pull requests, analyses, and suggestions

-- ============================================
-- USERS TABLE
-- ============================================
-- Purpose: Store authenticated users from GitHub OAuth
-- Key Design Decisions:
--   - github_id is the unique identifier from GitHub (BIGINT because GitHub IDs are large)
--   - access_token_hash: We hash the token for security, never store plain text
--   - UUID primary key: Better for distributed systems, harder to guess
--   - created_at/updated_at: Audit trail for debugging

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    avatar_url VARCHAR(500),
    access_token_hash VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookup by GitHub ID (used during OAuth)
CREATE INDEX idx_users_github_id ON users(github_id);

-- Why this design?
-- - UUID: Prevents enumeration attacks, better for microservices
-- - github_id UNIQUE: One GitHub account = one user
-- - access_token_hash: Security best practice
-- - email nullable: Not all GitHub users have public emails


-- ============================================
-- REPOSITORIES TABLE
-- ============================================
-- Purpose: Track which repositories are being monitored
-- Key Design Decisions:
--   - user_id: Foreign key to users (who added this repo)
--   - github_repo_id: GitHub's unique ID for the repository
--   - webhook_id: Store GitHub's webhook ID for easy management
--   - is_active: Soft enable/disable without deleting data
--   - settings JSONB: Flexible configuration without schema changes

CREATE TABLE repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id BIGINT UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,  -- e.g., "user/repo-name"
    description TEXT,
    language VARCHAR(50),
    webhook_id BIGINT,
    webhook_secret VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    settings JSONB DEFAULT '{}',  -- Flexible settings: {"autoAnalyze": true, "minConfidence": 70}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_repositories_user ON repositories(user_id);
CREATE INDEX idx_repositories_active ON repositories(is_active);
CREATE INDEX idx_repositories_github_id ON repositories(github_repo_id);

-- Why this design?
-- - CASCADE DELETE: If user is deleted, remove their repos
-- - JSONB for settings: Allows per-repo configuration without schema changes
-- - is_active: Pause monitoring without losing historical data
-- - webhook_secret: Each repo has its own secret for security


-- ============================================
-- PULL REQUESTS TABLE
-- ============================================
-- Purpose: Store metadata about pull requests we're analyzing
-- Key Design Decisions:
--   - Composite unique constraint: (repository_id, pr_number)
--   - status: Track PR lifecycle (open, closed, merged)
--   - Store GitHub URL for easy navigation
--   - Denormalize some data (title, author) for performance

CREATE TABLE pull_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    pr_number INTEGER NOT NULL,
    title VARCHAR(500),
    description TEXT,
    author VARCHAR(255),
    base_branch VARCHAR(255),
    head_branch VARCHAR(255),
    status VARCHAR(50) DEFAULT 'open',  -- open, closed, merged
    github_url VARCHAR(500),
    files_changed INTEGER,
    additions INTEGER,
    deletions INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Composite unique constraint: One PR number per repository
    CONSTRAINT uq_pr_repo_number UNIQUE (repository_id, pr_number)
);

-- Indexes for filtering and sorting
CREATE INDEX idx_pr_repository ON pull_requests(repository_id);
CREATE INDEX idx_pr_status ON pull_requests(status);
CREATE INDEX idx_pr_created ON pull_requests(created_at DESC);
CREATE INDEX idx_pr_author ON pull_requests(author);

-- Why this design?
-- - Composite unique: PR #42 can exist in multiple repos, but only once per repo
-- - Denormalized data: We cache title, author to avoid GitHub API calls
-- - files_changed, additions, deletions: Quick stats without parsing diff
-- - status: Filter open PRs, track lifecycle


-- ============================================
-- ANALYSES TABLE
-- ============================================
-- Purpose: Track the analysis process for each PR
-- Key Design Decisions:
--   - One-to-many with pull_requests (a PR can be analyzed multiple times)
--   - status: Track analysis lifecycle (pending, processing, completed, failed)
--   - duration_ms: Performance monitoring
--   - metadata JSONB: Store flexible analysis details

CREATE TABLE analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pull_request_id UUID NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'pending',  -- pending, processing, completed, failed
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms INTEGER,  -- Analysis duration in milliseconds
    files_analyzed INTEGER,
    error_message TEXT,
    metadata JSONB DEFAULT '{}',  -- Store analysis details: {"gemini_tokens": 1500, "patterns_found": 5}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for queries
CREATE INDEX idx_analyses_pr ON analyses(pull_request_id);
CREATE INDEX idx_analyses_status ON analyses(status);
CREATE INDEX idx_analyses_created ON analyses(created_at DESC);

-- Why this design?
-- - Multiple analyses per PR: Re-analyze when PR is updated
-- - duration_ms: Track performance, identify slow analyses
-- - metadata JSONB: Store API usage, tokens consumed, etc.
-- - error_message: Debug failed analyses


-- ============================================
-- SUGGESTIONS TABLE
-- ============================================
-- Purpose: Store AI-generated code review suggestions
-- Key Design Decisions:
--   - Many-to-one with analyses (one analysis produces many suggestions)
--   - category: Group suggestions (naming, architecture, performance, etc.)
--   - severity: Prioritize (critical, moderate, minor)
--   - confidence_score: AI's confidence (0-100)
--   - status: Track user action (pending, accepted, rejected, ignored)

CREATE TABLE suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL REFERENCES analyses(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    line_number INTEGER,
    line_end INTEGER,  -- For multi-line suggestions
    category VARCHAR(100),  -- naming, architecture, performance, security, testing, etc.
    severity VARCHAR(50),   -- critical, moderate, minor
    message TEXT NOT NULL,
    explanation TEXT,
    suggested_fix TEXT,
    confidence_score DECIMAL(5,2),  -- 0.00 to 100.00
    status VARCHAR(50) DEFAULT 'pending',  -- pending, accepted, rejected, ignored
    user_feedback TEXT,  -- Why user accepted/rejected
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for filtering and analytics
CREATE INDEX idx_suggestions_analysis ON suggestions(analysis_id);
CREATE INDEX idx_suggestions_status ON suggestions(status);
CREATE INDEX idx_suggestions_severity ON suggestions(severity);
CREATE INDEX idx_suggestions_category ON suggestions(category);
CREATE INDEX idx_suggestions_confidence ON suggestions(confidence_score DESC);

-- Why this design?
-- - line_end: Support multi-line suggestions
-- - category: Filter by type of issue
-- - severity: Prioritize critical issues
-- - confidence_score: Show only high-confidence suggestions
-- - user_feedback: Learn from user decisions (future ML improvement)


-- ============================================
-- CODE PATTERNS TABLE
-- ============================================
-- Purpose: Store learned patterns from approved PRs for RAG
-- Key Design Decisions:
--   - Store code snippets that were approved
--   - embedding_id: Reference to vector in Chroma DB
--   - frequency: Track how often pattern appears
--   - Used for similarity search in RAG pipeline

CREATE TABLE code_patterns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    pattern_type VARCHAR(100),  -- naming_convention, error_handling, class_structure, etc.
    language VARCHAR(50),
    code_snippet TEXT,
    context TEXT,  -- Description of when this pattern is used
    embedding_id VARCHAR(255),  -- Reference to Chroma vector ID
    frequency INTEGER DEFAULT 1,  -- How many times we've seen this pattern
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for pattern matching
CREATE INDEX idx_patterns_repo ON code_patterns(repository_id);
CREATE INDEX idx_patterns_type ON code_patterns(pattern_type);
CREATE INDEX idx_patterns_language ON code_patterns(language);
CREATE INDEX idx_patterns_frequency ON code_patterns(frequency DESC);

-- Why this design?
-- - embedding_id: Link to Chroma for similarity search
-- - frequency: Popular patterns are more important
-- - pattern_type: Categorize different kinds of patterns
-- - code_snippet: Store the actual code for display


-- ============================================
-- ANALYTICS EVENTS TABLE
-- ============================================
-- Purpose: Track events for analytics and metrics
-- Key Design Decisions:
--   - Generic event tracking for flexibility
--   - metadata JSONB: Store event-specific data
--   - Used for dashboard metrics and trends

CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,  -- pr_analyzed, suggestion_accepted, etc.
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    repository_id UUID REFERENCES repositories(id) ON DELETE SET NULL,
    metadata JSONB DEFAULT '{}',  -- Event-specific data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for analytics queries
CREATE INDEX idx_analytics_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_created ON analytics_events(created_at DESC);
CREATE INDEX idx_analytics_user ON analytics_events(user_id);
CREATE INDEX idx_analytics_repo ON analytics_events(repository_id);

-- Why this design?
-- - SET NULL on delete: Keep analytics even if user/repo deleted
-- - metadata JSONB: Flexible event data
-- - event_type: Group events for aggregation
-- - created_at index: Time-series queries


-- ============================================
-- TRIGGER: Auto-update updated_at
-- ============================================
-- Purpose: Automatically update updated_at timestamp on row modification
-- This is a common pattern to track when records change

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_repositories_updated_at BEFORE UPDATE ON repositories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pull_requests_updated_at BEFORE UPDATE ON pull_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_suggestions_updated_at BEFORE UPDATE ON suggestions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================
-- SUMMARY OF DESIGN DECISIONS
-- ============================================
-- 1. UUIDs for primary keys: Better for distributed systems, security
-- 2. CASCADE deletes: Clean up dependent data automatically
-- 3. JSONB for flexible data: Settings, metadata without schema changes
-- 4. Comprehensive indexes: Fast queries on common filters
-- 5. Audit timestamps: Track when data was created/modified
-- 6. Denormalization where needed: Cache data to reduce API calls
-- 7. Soft deletes via is_active: Keep historical data
-- 8. Foreign key constraints: Data integrity
-- 9. Unique constraints: Prevent duplicates
-- 10. Triggers for automation: Auto-update timestamps
