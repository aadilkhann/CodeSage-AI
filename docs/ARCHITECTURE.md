# System Architecture Document
## CodeSage AI - AI-Powered Code Review Assistant

**Version:** 1.0  
**Date:** November 24, 2025  
**Author:** Adil Khan

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Component Design](#3-component-design)
4. [Data Architecture](#4-data-architecture)
5. [API Design](#5-api-design)
6. [Security Architecture](#6-security-architecture)
7. [Deployment Architecture](#7-deployment-architecture)
8. [Technology Stack](#8-technology-stack)

---

## 1. Overview

### 1.1 Purpose
This document describes the technical architecture of CodeSage AI, an AI-powered code review assistant that provides context-aware suggestions based on repository history.

### 1.2 Scope
Covers the complete system architecture including:
- Microservices design
- Data flow and storage
- API specifications
- Security mechanisms
- Deployment strategy

### 1.3 Architectural Goals
- **Scalability**: Handle 1000+ PRs per day
- **Performance**: <2 minute analysis time
- **Reliability**: 99.5% uptime
- **Maintainability**: Clear separation of concerns
- **Cost-Efficiency**: Zero monthly operational costs

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        External Services                        │
│  ┌──────────┐         ┌──────────┐         ┌──────────┐         │
│  │  GitHub  │         │  Gemini  │         │ DuckDNS  │         │
│  │   API    │         │   API    │         │  (DNS)   │         │
│  └────┬─────┘         └────┬─────┘         └────┬─────┘         │
└───────┼────────────────────┼────────────────────┼───────────────┘
        │                    │                    │
        │                    │                    │
┌───────┼────────────────────┼────────────────────┼──────────────┐
│       │                    │                    │              │
│  ┌────▼────────────────────────────────────────▼────────────┐  │
│  │              Caddy Reverse Proxy (HTTPS)                 │  │
│  │         Auto SSL, Load Balancing, Rate Limiting          │  │
│  └──┬──────────────────┬──────────────────┬─────────────────┘  │
│     │                  │                  │                    │
│  ┌──▼──────┐    ┌──────▼──────┐    ┌─────▼──────┐              │
│  │ React   │    │   Spring    │    │  FastAPI   │              │
│  │Frontend │◄───┤    Boot     │◄───┤ AI Service │              │
│  │  (UI)   │    │  (Backend)  │    │  (ML/AI)   │              │
│  └─────────┘    └──┬────┬─────┘    └─────┬──────┘              │
│                    │    │                 │                    │
│               ┌────▼┐  ┌▼────┐      ┌────▼─────┐               │
│               │ PG  │  │Redis│      │  Chroma  │               │
│               │ SQL │  │Cache│      │VectorDB  │               │
│               └─────┘  └─────┘      └──────────┘               │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Monitoring: Prometheus + Grafana                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                    Self-Hosted Infrastructure
```

### 2.2 Architecture Style
**Microservices Architecture** with the following services:

1. **Frontend Service** (React)
   - User interface
   - Client-side routing
   - State management

2. **Backend Service** (Spring Boot)
   - Business logic
   - GitHub integration
   - Authentication & authorization
   - WebSocket server

3. **AI Service** (FastAPI)
   - Code analysis
   - Gemini API integration
   - RAG pipeline
   - Vector similarity search

4. **Data Layer**
   - PostgreSQL: Relational data
   - Redis: Caching
   - Chroma: Vector embeddings

---

## 3. Component Design

### 3.1 Frontend Architecture

```
frontend/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── common/          # Buttons, inputs, cards
│   │   ├── layout/          # Header, sidebar, footer
│   │   ├── dashboard/       # Dashboard-specific components
│   │   └── pr-analysis/     # PR analysis components
│   │
│   ├── features/            # Feature modules
│   │   ├── auth/            # Authentication
│   │   ├── repositories/    # Repository management
│   │   └── analysis/        # Analysis views
│   │
│   ├── hooks/               # Custom React hooks
│   ├── services/            # API clients
│   │   ├── api.ts           # Axios instance
│   │   ├── github.ts        # GitHub API calls
│   │   └── websocket.ts     # WebSocket client
│   │
│   ├── store/               # Redux store
│   │   ├── slices/          # Redux slices
│   │   └── index.ts         # Store configuration
│   │
│   ├── types/               # TypeScript types
│   └── utils/               # Utility functions
```

**Key Technologies:**
- React 18 with hooks
- TypeScript for type safety
- Redux Toolkit for state management
- React Query for server state
- TailwindCSS for styling
- Monaco Editor for code display

---

### 3.2 Backend Architecture

```
backend/
├── src/main/java/com/codesage/
│   ├── config/              # Configuration classes
│   │   ├── SecurityConfig.java
│   │   ├── WebSocketConfig.java
│   │   ├── RedisConfig.java
│   │   └── RestClientConfig.java
│   │
│   ├── controller/          # REST controllers
│   │   ├── AuthController.java
│   │   ├── RepositoryController.java
│   │   ├── PullRequestController.java
│   │   ├── AnalysisController.java
│   │   └── WebhookController.java
│   │
│   ├── service/             # Business logic
│   │   ├── AuthService.java
│   │   ├── GitHubService.java
│   │   ├── AnalysisService.java
│   │   ├── WebSocketService.java
│   │   └── CacheService.java
│   │
│   ├── repository/          # Data access
│   │   ├── UserRepository.java
│   │   ├── RepositoryRepository.java
│   │   ├── PullRequestRepository.java
│   │   └── SuggestionRepository.java
│   │
│   ├── model/               # JPA entities
│   │   ├── User.java
│   │   ├── Repository.java
│   │   ├── PullRequest.java
│   │   ├── Analysis.java
│   │   └── Suggestion.java
│   │
│   ├── security/            # Security components
│   │   ├── JwtTokenProvider.java
│   │   ├── OAuth2SuccessHandler.java
│   │   └── WebhookSignatureValidator.java
│   │
│   ├── dto/                 # Data transfer objects
│   └── exception/           # Custom exceptions
```

**Design Patterns:**
- **Layered Architecture**: Controller → Service → Repository
- **Dependency Injection**: Spring IoC container
- **Circuit Breaker**: Resilience4j for external APIs
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Clean API contracts

---

### 3.3 AI Service Architecture

```
ai-service/
├── app/
│   ├── routers/             # API endpoints
│   │   ├── analyze.py       # Analysis endpoints
│   │   ├── patterns.py      # Pattern management
│   │   └── health.py        # Health checks
│   │
│   ├── services/            # Business logic
│   │   ├── gemini_service.py      # Gemini API client
│   │   ├── rag_service.py         # RAG pipeline
│   │   ├── embedding_service.py   # Vector embeddings
│   │   └── pattern_detector.py    # Pattern matching
│   │
│   ├── models/              # Pydantic models
│   │   ├── schemas.py       # Request/response models
│   │   └── prompts.py       # Prompt templates
│   │
│   ├── utils/               # Utilities
│   │   ├── code_parser.py   # AST parsing
│   │   ├── diff_analyzer.py # Diff processing
│   │   └── rate_limiter.py  # API rate limiting
│   │
│   ├── config.py            # Configuration
│   └── main.py              # FastAPI app
```

**Key Components:**

1. **Gemini Service**
   - Manages Gemini API calls
   - Handles rate limiting
   - Formats prompts
   - Parses responses

2. **RAG Service**
   - Stores code patterns in Chroma
   - Performs similarity search
   - Builds context for prompts
   - Manages vector embeddings

3. **Pattern Detector**
   - Analyzes code structure
   - Identifies anti-patterns
   - Detects naming violations
   - Finds architectural issues

---

## 4. Data Architecture

### 4.1 Database Schema (PostgreSQL)

```sql
-- Users table
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

-- Repositories table
CREATE TABLE repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id BIGINT UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    description TEXT,
    language VARCHAR(50),
    webhook_id BIGINT,
    webhook_secret VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    settings JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Pull requests table
CREATE TABLE pull_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID REFERENCES repositories(id) ON DELETE CASCADE,
    pr_number INTEGER NOT NULL,
    title VARCHAR(500),
    description TEXT,
    author VARCHAR(255),
    base_branch VARCHAR(255),
    head_branch VARCHAR(255),
    status VARCHAR(50), -- open, closed, merged
    github_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(repository_id, pr_number)
);

-- Analyses table
CREATE TABLE analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pull_request_id UUID REFERENCES pull_requests(id) ON DELETE CASCADE,
    status VARCHAR(50), -- pending, processing, completed, failed
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms INTEGER,
    files_analyzed INTEGER,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Suggestions table
CREATE TABLE suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID REFERENCES analyses(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    line_number INTEGER,
    line_end INTEGER,
    category VARCHAR(100), -- naming, architecture, performance, security, etc.
    severity VARCHAR(50), -- critical, moderate, minor
    message TEXT NOT NULL,
    explanation TEXT,
    suggested_fix TEXT,
    confidence_score DECIMAL(5,2), -- 0-100
    status VARCHAR(50), -- pending, accepted, rejected, ignored
    user_feedback TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Code patterns table (for learning)
CREATE TABLE code_patterns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id UUID REFERENCES repositories(id) ON DELETE CASCADE,
    pattern_type VARCHAR(100), -- naming, structure, error_handling, etc.
    language VARCHAR(50),
    code_snippet TEXT,
    context TEXT,
    embedding_id VARCHAR(255), -- Reference to Chroma vector
    frequency INTEGER DEFAULT 1,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Analytics table
CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100), -- pr_analyzed, suggestion_accepted, etc.
    user_id UUID REFERENCES users(id),
    repository_id UUID REFERENCES repositories(id),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_repositories_user ON repositories(user_id);
CREATE INDEX idx_repositories_active ON repositories(is_active);
CREATE INDEX idx_pr_repository ON pull_requests(repository_id);
CREATE INDEX idx_pr_status ON pull_requests(status);
CREATE INDEX idx_pr_created ON pull_requests(created_at DESC);
CREATE INDEX idx_analyses_pr ON analyses(pull_request_id);
CREATE INDEX idx_analyses_status ON analyses(status);
CREATE INDEX idx_suggestions_analysis ON suggestions(analysis_id);
CREATE INDEX idx_suggestions_status ON suggestions(status);
CREATE INDEX idx_suggestions_severity ON suggestions(severity);
CREATE INDEX idx_patterns_repo ON code_patterns(repository_id);
CREATE INDEX idx_patterns_type ON code_patterns(pattern_type);
CREATE INDEX idx_analytics_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_created ON analytics_events(created_at DESC);
```

### 4.2 Redis Cache Strategy

**Cache Keys:**
```
github:pr:{prId}              # PR metadata (TTL: 1 hour)
github:diff:{prId}            # PR diff (TTL: 1 hour)
github:user:{userId}          # User profile (TTL: 24 hours)
github:repo:{repoId}          # Repository info (TTL: 24 hours)
analysis:result:{analysisId}  # Analysis results (TTL: 7 days)
rate_limit:gemini:{date}      # Gemini API usage (TTL: 24 hours)
```

**Caching Strategy:**
- **Cache-Aside**: Application manages cache
- **Write-Through**: Update cache on write
- **TTL-based expiration**: Prevent stale data
- **Eviction Policy**: LRU (Least Recently Used)

### 4.3 Vector Database (Chroma)

**Collections:**
```
code_patterns:
  - Embeddings of approved code changes
  - Metadata: language, file_path, pattern_type
  - Used for similarity search in RAG

Dimensions: 768 (text-embedding-004)
Distance Metric: Cosine similarity
Index Type: HNSW (Hierarchical Navigable Small World)
```

---

## 5. API Design

### 5.1 REST API Endpoints

#### Authentication
```
POST   /api/v1/auth/github/login      # Initiate OAuth flow
GET    /api/v1/auth/github/callback   # OAuth callback
POST   /api/v1/auth/refresh            # Refresh JWT token
POST   /api/v1/auth/logout             # Logout
```

#### Repositories
```
GET    /api/v1/repositories            # List user repositories
POST   /api/v1/repositories            # Add repository
GET    /api/v1/repositories/{id}       # Get repository details
PUT    /api/v1/repositories/{id}       # Update repository settings
DELETE /api/v1/repositories/{id}       # Remove repository
POST   /api/v1/repositories/{id}/sync  # Sync with GitHub
```

#### Pull Requests
```
GET    /api/v1/pull-requests                    # List PRs
GET    /api/v1/pull-requests/{id}               # Get PR details
POST   /api/v1/pull-requests/{id}/analyze       # Trigger analysis
GET    /api/v1/pull-requests/{id}/analysis      # Get analysis results
```

#### Suggestions
```
GET    /api/v1/suggestions                      # List suggestions
PUT    /api/v1/suggestions/{id}/accept          # Accept suggestion
PUT    /api/v1/suggestions/{id}/reject          # Reject suggestion
POST   /api/v1/suggestions/{id}/feedback        # Provide feedback
```

#### Analytics
```
GET    /api/v1/analytics/overview               # Dashboard metrics
GET    /api/v1/analytics/trends                 # Time-series data
GET    /api/v1/analytics/team                   # Team statistics
```

#### Webhooks
```
POST   /api/v1/webhooks/github                  # GitHub webhook receiver
```

### 5.2 WebSocket API

**Connection:**
```
ws://localhost:8080/ws
```

**Message Types:**
```javascript
// Progress update
{
  "type": "progress",
  "prId": "uuid",
  "progress": 45,
  "message": "Analyzing file 3 of 10"
}

// New suggestion
{
  "type": "suggestion",
  "prId": "uuid",
  "suggestion": { /* suggestion object */ }
}

// Analysis complete
{
  "type": "complete",
  "prId": "uuid",
  "totalSuggestions": 5
}

// Error
{
  "type": "error",
  "prId": "uuid",
  "message": "Analysis failed"
}
```

### 5.3 AI Service API

```
POST   /ai/analyze/pr                   # Analyze PR
POST   /ai/analyze/file                 # Analyze single file
POST   /ai/patterns/add                 # Add code pattern
GET    /ai/patterns/similar             # Find similar patterns
GET    /ai/health                       # Health check
```

---

## 6. Security Architecture

### 6.1 Authentication Flow

```
1. User clicks "Login with GitHub"
2. Frontend redirects to GitHub OAuth
3. User authorizes application
4. GitHub redirects to backend callback
5. Backend exchanges code for access token
6. Backend creates JWT token
7. Backend returns JWT to frontend
8. Frontend stores JWT in localStorage
9. Frontend includes JWT in all API requests
```

### 6.2 Authorization

**JWT Token Structure:**
```json
{
  "sub": "user-uuid",
  "username": "john_doe",
  "githubId": 12345,
  "roles": ["USER"],
  "iat": 1700000000,
  "exp": 1700003600
}
```

**Access Control:**
- Users can only access their own repositories
- Repository owners can manage settings
- Analysis results visible to repository members

### 6.3 Security Measures

1. **HTTPS Only** in production
2. **JWT Expiration**: 1 hour (refresh: 7 days)
3. **Webhook Signature Verification**: HMAC-SHA256
4. **Rate Limiting**: 100 requests/minute per user
5. **Input Validation**: All inputs sanitized
6. **SQL Injection Prevention**: Parameterized queries
7. **XSS Prevention**: Content Security Policy
8. **CORS**: Whitelist allowed origins
9. **Secrets Management**: Environment variables
10. **No Source Code Storage**: Only embeddings

---

## 7. Deployment Architecture

### 7.1 Self-Hosted Deployment

```
┌─────────────────────────────────────────────────────┐
│              Internet (Port 80/443)                 │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│                Router (Port Forwarding)             │
│           80 → 192.168.1.100:80                     │
│           443 → 192.168.1.100:443                   │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│         Spare PC (192.168.1.100)                    │
│  ┌──────────────────────────────────────────────┐   │
│  │   Caddy (Auto HTTPS via Let's Encrypt)       │   │
│  └──────────────────┬───────────────────────────┘   │
│                     │                               │
│  ┌──────────────────┼───────────────────────────┐   │
│  │   Docker Compose Network                     │   │
│  │                  │                           │   │
│  │  ┌───────┐  ┌───▼────┐  ┌─────────┐          │   │
│  │  │Frontend│ │Backend │  │AI Service│         │   │
│  │  └───────┘  └───┬────┘  └────┬────┘          │   │
│  │                 │            │               │   │
│  │  ┌──────────────┼────────────┼──────────┐    │   │
│  │  │  PostgreSQL  Redis    Chroma         │    │   │
│  │  └──────────────────────────────────────┘    │   │
│  │                                              │   │
│  │  ┌──────────────────────────────────────┐    │   │
│  │  │  Prometheus + Grafana                │    │   │
│  │  └──────────────────────────────────────┘    │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### 7.2 Container Orchestration

**Docker Compose Services:**
- **caddy**: Reverse proxy with auto-HTTPS
- **frontend**: React app (Nginx in production)
- **backend**: Spring Boot application
- **ai-service**: FastAPI application
- **postgres**: PostgreSQL database
- **redis**: Redis cache
- **prometheus**: Metrics collection
- **grafana**: Monitoring dashboards

**Volumes:**
- `postgres_data`: Database persistence
- `redis_data`: Cache persistence
- `chroma_data`: Vector database persistence
- `caddy_data`: SSL certificates
- `prometheus_data`: Metrics history
- `grafana_data`: Dashboard configs

### 7.3 Monitoring & Logging

**Prometheus Metrics:**
- HTTP request rate and latency
- Database connection pool stats
- Cache hit/miss rates
- Gemini API usage
- Analysis completion times
- Error rates

**Grafana Dashboards:**
- System overview
- API performance
- Database metrics
- AI service metrics
- Business metrics (PRs analyzed, suggestions)

**Logging:**
- Structured JSON logs
- Log levels: DEBUG, INFO, WARN, ERROR
- Centralized logging to files
- Log rotation (30 days retention)

---

## 8. Technology Stack

### 8.1 Backend Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | Spring Boot | 3.2.x | Application framework |
| Language | Java | 17 | Programming language |
| Build Tool | Maven | 3.9+ | Dependency management |
| Database | PostgreSQL | 15 | Relational database |
| Cache | Redis | 7 | In-memory cache |
| ORM | Spring Data JPA | - | Database access |
| Migration | Flyway | - | Schema versioning |
| Security | Spring Security | - | Authentication/Authorization |
| WebSocket | Spring WebSocket | - | Real-time communication |
| HTTP Client | WebClient | - | External API calls |
| Circuit Breaker | Resilience4j | 2.1 | Fault tolerance |
| Monitoring | Micrometer | - | Metrics collection |

### 8.2 AI Service Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | FastAPI | 0.104+ | Web framework |
| Language | Python | 3.11 | Programming language |
| LLM | Gemini 2.5 Flash | - | Code analysis |
| Vector DB | Chroma | 0.4+ | Embeddings storage |
| Embeddings | text-embedding-004 | - | Vector generation |
| Code Parsing | Tree-sitter | 0.20+ | AST parsing |
| HTTP Client | httpx | - | Async HTTP |
| Validation | Pydantic | 2.5+ | Data validation |

### 8.3 Frontend Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | React | 18 | UI framework |
| Language | TypeScript | 5.2+ | Type safety |
| Build Tool | Vite | 5.0+ | Fast builds |
| State Management | Redux Toolkit | 2.0+ | Global state |
| Server State | React Query | 5.0+ | API caching |
| Routing | React Router | 6.20+ | Client routing |
| Styling | TailwindCSS | 3.3+ | Utility CSS |
| Code Editor | Monaco Editor | 4.6+ | Code display |
| WebSocket | STOMP.js | 7.0+ | Real-time updates |
| HTTP Client | Axios | 1.6+ | API calls |

### 8.4 Infrastructure Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Containerization | Docker | 24+ | Application packaging |
| Orchestration | Docker Compose | 2.0+ | Multi-container apps |
| Reverse Proxy | Caddy | 2 | HTTPS termination |
| Monitoring | Prometheus | latest | Metrics collection |
| Dashboards | Grafana | latest | Visualization |
| DNS | DuckDNS | - | Dynamic DNS |
| SSL | Let's Encrypt | - | Free SSL certificates |

---

## 9. Data Flow Diagrams

### 9.1 PR Analysis Flow

```
┌─────────┐
│ GitHub  │
│Webhook  │
└────┬────┘
     │ 1. PR Event
     ▼
┌─────────────┐
│   Backend   │
│  Webhook    │
│  Handler    │
└────┬────────┘
     │ 2. Validate & Store PR
     ▼
┌─────────────┐
│ PostgreSQL  │
└────┬────────┘
     │ 3. Fetch PR Details
     ▼
┌─────────────┐
│   GitHub    │
│     API     │
└────┬────────┘
     │ 4. Get Diff
     ▼
┌─────────────┐
│   Backend   │
│  Analysis   │
│  Service    │
└────┬────────┘
     │ 5. Send to AI Service
     ▼
┌─────────────┐
│ AI Service  │
│   RAG       │
└────┬────────┘
     │ 6. Find Similar Patterns
     ▼
┌─────────────┐
│   Chroma    │
│  VectorDB   │
└────┬────────┘
     │ 7. Build Context
     ▼
┌─────────────┐
│ AI Service  │
│   Gemini    │
└────┬────────┘
     │ 8. Analyze Code
     ▼
┌─────────────┐
│   Gemini    │
│     API     │
└────┬────────┘
     │ 9. Return Suggestions
     ▼
┌─────────────┐
│ AI Service  │
│   Parser    │
└────┬────────┘
     │ 10. Store Suggestions
     ▼
┌─────────────┐
│ PostgreSQL  │
└────┬────────┘
     │ 11. Notify via WebSocket
     ▼
┌─────────────┐
│   Backend   │
│  WebSocket  │
└────┬────────┘
     │ 12. Real-time Update
     ▼
┌─────────────┐
│  Frontend   │
│     UI      │
└─────────────┘
```

### 9.2 Authentication Flow

```
┌─────────┐
│ Frontend│
└────┬────┘
     │ 1. Click "Login with GitHub"
     ▼
┌─────────────┐
│   Backend   │
│    Auth     │
└────┬────────┘
     │ 2. Redirect to GitHub
     ▼
┌─────────────┐
│   GitHub    │
│    OAuth    │
└────┬────────┘
     │ 3. User Authorizes
     ▼
┌─────────────┐
│   Backend   │
│  Callback   │
└────┬────────┘
     │ 4. Exchange Code for Token
     ▼
┌─────────────┐
│   GitHub    │
│     API     │
└────┬────────┘
     │ 5. Return Access Token
     ▼
┌─────────────┐
│   Backend   │
│     JWT     │
└────┬────────┘
     │ 6. Create JWT
     ▼
┌─────────────┐
│ PostgreSQL  │
└────┬────────┘
     │ 7. Store User
     ▼
┌─────────────┐
│  Frontend   │
│   Storage   │
└─────────────┘
     8. Store JWT in localStorage
```

---

## 10. Scalability Considerations

### 10.1 Horizontal Scaling
- **Frontend**: Multiple instances behind load balancer
- **Backend**: Stateless design allows multiple instances
- **AI Service**: Queue-based processing for parallel analysis
- **Database**: Read replicas for query scaling

### 10.2 Vertical Scaling
- **Database**: Increase CPU/RAM for complex queries
- **AI Service**: GPU acceleration for faster inference
- **Cache**: Larger Redis instance for more data

### 10.3 Performance Optimizations
- **Caching**: Redis for frequently accessed data
- **Database Indexing**: Optimized queries
- **Connection Pooling**: Reuse database connections
- **Async Processing**: Non-blocking I/O
- **CDN**: Static asset delivery (future)

---

## 11. Disaster Recovery

### 11.1 Backup Strategy
- **Database**: Daily automated backups
- **Vector DB**: Weekly backups of Chroma data
- **Configuration**: Version controlled
- **Retention**: 30 days

### 11.2 Recovery Procedures
- **RTO (Recovery Time Objective)**: 1 hour
- **RPO (Recovery Point Objective)**: 24 hours
- **Automated restore scripts**
- **Documented recovery steps**

---

## 12. Future Enhancements

### 12.1 Short-term (3-6 months)
- Message queue (RabbitMQ) for async processing
- API Gateway (Kong/Nginx) for centralized routing
- Elasticsearch for advanced search
- CI/CD pipeline automation

### 12.2 Long-term (6-12 months)
- Kubernetes deployment
- Multi-region support
- Advanced analytics with ML
- Mobile applications

---

**Document Control**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-24 | Architecture Team | Initial version |

---

**Approval**

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Solution Architect | - | Approved | 2025-11-24 |
| Tech Lead | - | Approved | 2025-11-24 |
| Security Lead | - | Approved | 2025-11-24 |
