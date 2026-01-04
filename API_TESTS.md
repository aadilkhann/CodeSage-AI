# API Test Commands - CodeSage AI MVP

This file contains curl commands to test the MVP without a frontend.

## Prerequisites

```bash
# Make sure all services are running
./start.sh

# Export these for easier testing
export API_URL="http://localhost:8080"
export AI_URL="http://localhost:8000"
```

## 🏥 Health Checks

```bash
# AI Service health
curl $AI_URL/health | jq

# Backend health
curl $API_URL/actuator/health | jq

# Check PostgreSQL
docker exec -it codesage-postgres psql -U codesage -d codesage -c "\dt"

# Check Redis
docker exec -it codesage-redis redis-cli ping
```

## 🤖 AI Service Tests (No Auth Required)

### Test PR Analysis

```bash
curl -X POST $AI_URL/ai/analyze/pr \
  -H "Content-Type: application/json" \
  -d '{
    "repository_id": "test-repo-123",
    "pr_number": 1,
    "diff": "diff --git a/UserService.java b/UserService.java\nindex 1234567..abcdefg 100644\n--- a/UserService.java\n+++ b/UserService.java\n@@ -10,7 +10,7 @@ public class UserService {\n-    public void oldMethod() {\n+    public void badMethod() {\n+        int x = 1;  // Non-descriptive variable\n+        for(int i=0;i<10;i++) {  // No spaces\n+            System.out.println(x);\n+        }\n     }\n }",
    "files": [
      {
        "filename": "UserService.java",
        "status": "modified",
        "additions": 5,
        "deletions": 2
      }
    ],
    "language": "java"
  }' | jq
```

### Test File Analysis

```bash
curl -X POST "$AI_URL/ai/analyze/file?repository_id=test-repo&file_path=Example.java&language=java" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Example {\n    public void m() {\n        int x = 1;\n        String s = \"test\";\n    }\n}"
  }' | jq
```

### Add Code Pattern

```bash
curl -X POST $AI_URL/ai/patterns/add \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public List<User> findActiveUsers() {\n    return userRepository.findByIsActiveTrue();\n}",
    "repository_id": "test-repo-123",
    "file_path": "UserService.java",
    "language": "java",
    "category": "architecture",
    "description": "Repository pattern for finding active users"
  }' | jq
```

### Find Similar Patterns

```bash
curl -X POST $AI_URL/ai/patterns/similar \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public List<User> getUsers() { return userRepository.findAll(); }",
    "repository_id": "test-repo-123",
    "language": "java",
    "limit": 5
  }' | jq
```

## 🔐 Backend Tests (Requires Auth)

### Step 1: Start OAuth Flow

**Open in browser:**
```
http://localhost:8080/oauth2/authorize/github
```

This will:
1. Redirect to GitHub login
2. Ask for authorization
3. Redirect back with JWT token

**Capture the JWT token** from the callback URL or response headers.

### Step 2: Set Your Token

```bash
export JWT_TOKEN="your-jwt-token-here"
```

### Step 3: Test Authenticated Endpoints

#### List Repositories

```bash
curl -X GET $API_URL/api/v1/repositories \
  -H "Authorization: Bearer $JWT_TOKEN" | jq
```

#### Add Repository (Requires GitHub repo access)

```bash
curl -X POST $API_URL/api/v1/repositories \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "githubRepoId": 123456789,
    "fullName": "yourusername/your-repo",
    "description": "Test repository",
    "language": "Java"
  }' | jq
```

#### List Pull Requests

```bash
curl -X GET "$API_URL/api/v1/pull-requests?page=0&size=10" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq
```

#### Get PR Details

```bash
# Replace {pr-uuid} with actual UUID
curl -X GET $API_URL/api/v1/pull-requests/{pr-uuid} \
  -H "Authorization: Bearer $JWT_TOKEN" | jq
```

#### Trigger Manual Analysis

```bash
# Replace {pr-uuid} with actual UUID
curl -X POST $API_URL/api/v1/pull-requests/{pr-uuid}/analyze \
  -H "Authorization: Bearer $JWT_TOKEN" | jq
```

## 🔄 WebSocket Test

You can test WebSocket connections using `wscat`:

```bash
# Install wscat
npm install -g wscat

# Connect to WebSocket
wscat -c ws://localhost:8080/ws

# After connecting, subscribe to analysis updates:
# Send:
{
  "destination": "/app/subscribe",
  "body": "{\"analysisId\":\"your-analysis-uuid\"}"
}

# You'll receive real-time updates as:
# - Progress updates (0-100%)
# - New suggestions
# - Completion notifications
```

## 🧪 GitHub Webhook Test

To test webhook handling, you need to:

1. **Setup ngrok** (for local testing):
```bash
ngrok http 8080
```

2. **Configure GitHub webhook** in your repo settings:
   - Payload URL: `https://your-ngrok-url.ngrok.io/api/v1/webhooks/github`
   - Content type: `application/json`
   - Secret: Your `GITHUB_WEBHOOK_SECRET` from `.env`
   - Events: Select "Pull requests"

3. **Test by creating a PR** in your repository

4. **Check webhook received**:
```bash
tail -f logs/backend.log | grep webhook
```

## 📊 Database Queries

```bash
# View all users
docker exec -it codesage-postgres psql -U codesage -d codesage -c "SELECT * FROM users;"

# View repositories
docker exec -it codesage-postgres psql -U codesage -d codesage -c "SELECT id, full_name, is_active FROM repositories;"

# View pull requests
docker exec -it codesage-postgres psql -U codesage -d codesage -c "SELECT id, pr_number, title, status FROM pull_requests LIMIT 10;"

# View analyses
docker exec -it codesage-postgres psql -U codesage -d codesage -c "SELECT id, status, files_analyzed, duration_ms FROM analyses LIMIT 10;"

# View suggestions
docker exec -it codesage-postgres psql -U codesage -d codesage -c "SELECT id, file_path, category, severity, message FROM suggestions LIMIT 10;"
```

## 🐛 Troubleshooting

### Check Logs

```bash
# Backend logs
tail -f logs/backend.log

# AI service logs
tail -f logs/ai-service.log

# Docker logs
docker logs codesage-postgres
docker logs codesage-redis
```

### Clear Cache

```bash
# Clear Redis cache
docker exec -it codesage-redis redis-cli FLUSHALL

# Clear ChromaDB
rm -rf ai-service/chroma_data
```

### Reset Database

```bash
# Stop services
./stop.sh

# Remove database volume
docker volume rm ai-powered-code-review-assistant_postgres_data

# Restart - Flyway will recreate schema
./start.sh
```

## ✅ Success Indicators

If everything is working:

1. ✅ All health checks return `200 OK`
2. ✅ AI service returns suggestions for sample diff
3. ✅ OAuth login redirects to GitHub
4. ✅ Backend returns empty repository list (or your repos if you added them)
5. ✅ WebSocket connection established
6. ✅ Database queries return empty tables (initially)

## 📝 Notes

- **No Frontend**: The MVP backend is complete but there's no UI
- **Manual Testing**: Use curl, Postman, or Insomnia
- **GitHub OAuth**: Requires real GitHub account and OAuth app
- **Webhook**: Requires public URL (ngrok for local testing)
- **AI Analysis**: Works immediately via direct API calls
