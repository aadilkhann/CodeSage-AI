# API Documentation
## CodeSage AI - REST API Reference

**Version:** 1.0  
**Base URL:** `http://localhost:8080/api/v1`  
**Production URL:** `https://your-domain.duckdns.org/api/v1`

---

## Table of Contents

1. [Authentication](#authentication)
2. [Repositories](#repositories)
3. [Pull Requests](#pull-requests)
4. [Analyses](#analyses)
5. [Suggestions](#suggestions)
6. [Analytics](#analytics)
7. [Webhooks](#webhooks)
8. [Error Handling](#error-handling)

---

## Authentication

### POST /auth/github/login
Initiate GitHub OAuth flow

**Response:**
```json
{
  "url": "https://github.com/login/oauth/authorize?client_id=..."
}
```

### GET /auth/github/callback
OAuth callback endpoint (handled by backend)

**Query Parameters:**
- `code` (string, required): OAuth authorization code

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "username": "john_doe",
    "email": "john@example.com",
    "avatarUrl": "https://avatars.githubusercontent.com/..."
  }
}
```

### POST /auth/refresh
Refresh JWT token

**Request Headers:**
```
Authorization: Bearer <refresh_token>
```

**Response:**
```json
{
  "accessToken": "new_access_token",
  "expiresIn": 3600
}
```

### POST /auth/logout
Logout user

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "message": "Logged out successfully"
}
```

---

## Repositories

### GET /repositories
List user's repositories

**Request Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
- `page` (integer, optional, default: 0): Page number
- `size` (integer, optional, default: 20): Page size
- `search` (string, optional): Search query
- `active` (boolean, optional): Filter by active status

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "githubRepoId": 123456,
      "fullName": "user/repo-name",
      "description": "Repository description",
      "language": "Java",
      "isActive": true,
      "webhookConfigured": true,
      "createdAt": "2025-11-24T10:00:00Z",
      "updatedAt": "2025-11-24T10:00:00Z"
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### POST /repositories
Add repository to CodeSage

**Request Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "githubRepoId": 123456,
  "fullName": "user/repo-name"
}
```

**Response:**
```json
{
  "id": "uuid",
  "githubRepoId": 123456,
  "fullName": "user/repo-name",
  "description": "Repository description",
  "language": "Java",
  "isActive": true,
  "webhookConfigured": false,
  "createdAt": "2025-11-24T10:00:00Z"
}
```

### GET /repositories/{id}
Get repository details

**Response:**
```json
{
  "id": "uuid",
  "githubRepoId": 123456,
  "fullName": "user/repo-name",
  "description": "Repository description",
  "language": "Java",
  "isActive": true,
  "webhookConfigured": true,
  "settings": {
    "autoAnalyze": true,
    "minConfidenceScore": 70,
    "enabledCategories": ["naming", "architecture", "performance"]
  },
  "statistics": {
    "totalPRs": 150,
    "analyzedPRs": 145,
    "totalSuggestions": 523,
    "acceptedSuggestions": 412
  },
  "createdAt": "2025-11-24T10:00:00Z",
  "updatedAt": "2025-11-24T10:00:00Z"
}
```

### PUT /repositories/{id}
Update repository settings

**Request Body:**
```json
{
  "isActive": true,
  "settings": {
    "autoAnalyze": true,
    "minConfidenceScore": 75,
    "enabledCategories": ["naming", "architecture"]
  }
}
```

### DELETE /repositories/{id}
Remove repository

**Response:**
```json
{
  "message": "Repository removed successfully"
}
```

### POST /repositories/{id}/sync
Sync repository with GitHub

**Response:**
```json
{
  "message": "Sync initiated",
  "syncId": "uuid"
}
```

---

## Pull Requests

### GET /pull-requests
List pull requests

**Query Parameters:**
- `repositoryId` (uuid, optional): Filter by repository
- `status` (string, optional): Filter by status (open, closed, merged)
- `page` (integer, optional, default: 0)
- `size` (integer, optional, default: 20)

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "repositoryId": "uuid",
      "prNumber": 42,
      "title": "Add new feature",
      "author": "john_doe",
      "status": "open",
      "baseBranch": "main",
      "headBranch": "feature/new-feature",
      "githubUrl": "https://github.com/user/repo/pull/42",
      "hasAnalysis": true,
      "analysisStatus": "completed",
      "suggestionCount": 5,
      "createdAt": "2025-11-24T10:00:00Z",
      "updatedAt": "2025-11-24T10:30:00Z"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

### GET /pull-requests/{id}
Get pull request details

**Response:**
```json
{
  "id": "uuid",
  "repositoryId": "uuid",
  "repository": {
    "fullName": "user/repo-name"
  },
  "prNumber": 42,
  "title": "Add new feature",
  "description": "This PR adds...",
  "author": "john_doe",
  "status": "open",
  "baseBranch": "main",
  "headBranch": "feature/new-feature",
  "githubUrl": "https://github.com/user/repo/pull/42",
  "filesChanged": 5,
  "additions": 150,
  "deletions": 30,
  "createdAt": "2025-11-24T10:00:00Z",
  "updatedAt": "2025-11-24T10:30:00Z"
}
```

### POST /pull-requests/{id}/analyze
Trigger manual analysis

**Response:**
```json
{
  "message": "Analysis started",
  "analysisId": "uuid"
}
```

### GET /pull-requests/{id}/analysis
Get analysis results

**Response:**
```json
{
  "id": "uuid",
  "pullRequestId": "uuid",
  "status": "completed",
  "startedAt": "2025-11-24T10:00:00Z",
  "completedAt": "2025-11-24T10:02:00Z",
  "durationMs": 120000,
  "filesAnalyzed": 5,
  "suggestions": [
    {
      "id": "uuid",
      "filePath": "src/main/java/com/example/Service.java",
      "lineNumber": 42,
      "category": "naming",
      "severity": "minor",
      "message": "Variable name 'x' is not descriptive",
      "explanation": "Based on similar code in this repository...",
      "suggestedFix": "int userCount = users.size();",
      "confidenceScore": 85,
      "status": "pending"
    }
  ],
  "summary": {
    "totalSuggestions": 5,
    "bySeverity": {
      "critical": 0,
      "moderate": 2,
      "minor": 3
    },
    "byCategory": {
      "naming": 2,
      "architecture": 1,
      "performance": 2
    }
  }
}
```

---

## Suggestions

### GET /suggestions
List suggestions

**Query Parameters:**
- `analysisId` (uuid, optional): Filter by analysis
- `status` (string, optional): Filter by status
- `severity` (string, optional): Filter by severity
- `category` (string, optional): Filter by category

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "analysisId": "uuid",
      "filePath": "src/main/java/Service.java",
      "lineNumber": 42,
      "category": "naming",
      "severity": "minor",
      "message": "Variable name 'x' is not descriptive",
      "confidenceScore": 85,
      "status": "pending",
      "createdAt": "2025-11-24T10:00:00Z"
    }
  ]
}
```

### PUT /suggestions/{id}/accept
Accept suggestion

**Response:**
```json
{
  "id": "uuid",
  "status": "accepted",
  "updatedAt": "2025-11-24T10:05:00Z"
}
```

### PUT /suggestions/{id}/reject
Reject suggestion

**Request Body:**
```json
{
  "reason": "Not applicable in this context"
}
```

**Response:**
```json
{
  "id": "uuid",
  "status": "rejected",
  "userFeedback": "Not applicable in this context",
  "updatedAt": "2025-11-24T10:05:00Z"
}
```

---

## Analytics

### GET /analytics/overview
Get dashboard metrics

**Query Parameters:**
- `repositoryId` (uuid, optional): Filter by repository
- `startDate` (date, optional): Start date (ISO 8601)
- `endDate` (date, optional): End date (ISO 8601)

**Response:**
```json
{
  "period": {
    "startDate": "2025-11-01",
    "endDate": "2025-11-24"
  },
  "metrics": {
    "totalPRsAnalyzed": 150,
    "totalSuggestions": 523,
    "suggestionAcceptanceRate": 78.5,
    "averageAnalysisTime": 95000,
    "timeSaved": 450
  },
  "trends": {
    "prsAnalyzed": [
      {"date": "2025-11-01", "count": 5},
      {"date": "2025-11-02", "count": 7}
    ],
    "suggestionAcceptance": [
      {"date": "2025-11-01", "rate": 75.0},
      {"date": "2025-11-02", "rate": 80.0}
    ]
  }
}
```

### GET /analytics/categories
Get suggestion breakdown by category

**Response:**
```json
{
  "categories": [
    {
      "name": "naming",
      "count": 150,
      "acceptanceRate": 85.0
    },
    {
      "name": "architecture",
      "count": 120,
      "acceptanceRate": 70.0
    }
  ]
}
```

---

## Webhooks

### POST /webhooks/github
GitHub webhook receiver

**Request Headers:**
```
X-GitHub-Event: pull_request
X-Hub-Signature-256: sha256=...
Content-Type: application/json
```

**Request Body:**
```json
{
  "action": "opened",
  "pull_request": {
    "number": 42,
    "title": "Add new feature",
    ...
  },
  "repository": {
    "id": 123456,
    "full_name": "user/repo"
  }
}
```

**Response:**
```json
{
  "message": "Webhook processed successfully"
}
```

---

## Error Handling

### Error Response Format

```json
{
  "timestamp": "2025-11-24T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "path": "/api/v1/repositories",
  "errors": [
    {
      "field": "githubRepoId",
      "message": "must not be null"
    }
  ]
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created |
| 204 | No Content | Request successful, no content |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource already exists |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

---

## Rate Limiting

**Limits:**
- 100 requests per minute per user
- 1000 requests per hour per user

**Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1700000000
```

---

## Pagination

**Query Parameters:**
- `page`: Page number (0-indexed)
- `size`: Items per page (max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Response Headers:**
```
X-Total-Count: 150
X-Total-Pages: 8
X-Current-Page: 0
X-Page-Size: 20
```

---

## WebSocket API

**Connection:**
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  // Subscribe to PR analysis updates
  stompClient.subscribe('/topic/pr/{prId}/progress', function(message) {
    const progress = JSON.parse(message.body);
    console.log(progress);
  });
});
```

**Message Types:**
- `/topic/pr/{prId}/progress`: Analysis progress updates
- `/topic/pr/{prId}/suggestions`: New suggestions
- `/topic/pr/{prId}/complete`: Analysis complete
- `/topic/pr/{prId}/error`: Analysis error

---

**Last Updated:** 2025-11-24  
**Version:** 1.0
