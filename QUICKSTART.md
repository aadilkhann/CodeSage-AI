# 🚀 Quick Start Guide - CodeSage AI MVP

## ✅ What's Implemented

**Backend (Spring Boot)**
- ✅ Complete authentication (GitHub OAuth + JWT)
- ✅ GitHub API integration with webhooks
- ✅ Analysis pipeline with async processing
- ✅ WebSocket for real-time updates
- ✅ Redis caching
- ✅ All REST endpoints

**AI Service (FastAPI)**
- ✅ Gemini 2.5 Flash integration
- ✅ ChromaDB RAG for code patterns
- ✅ PR analysis endpoint
- ✅ Pattern management

**What's NOT Ready**
- ❌ Frontend (React app not implemented)
- ❌ Some optional utilities (pattern detector, embedding service)

## 📋 Prerequisites

1. **Java 17+** - `java -version`
2. **Maven 3.8+** - `mvn -version`
3. **Python 3.11+** - `python3 --version`
4. **Docker & Docker Compose** - `docker --version`
5. **Gemini API Key** - Get free from https://aistudio.google.com/app/apikey
6. **GitHub OAuth App** - Register at https://github.com/settings/developers

## 🔧 Setup Steps

### 1. Clone and Navigate
```bash
cd /Users/adii/Builds/AI-Powered-Code-Review-Assistant
```

### 2. Setup Environment Variables
```bash
# Copy example file
cp .env.example .env

# Edit .env and fill in:
nano .env
```

**Required variables:**
```bash
GEMINI_API_KEY=your_actual_gemini_key
GITHUB_CLIENT_ID=your_github_oauth_id
GITHUB_CLIENT_SECRET=your_github_oauth_secret
JWT_SECRET=$(openssl rand -hex 32)
GITHUB_WEBHOOK_SECRET=$(openssl rand -hex 32)
DB_PASSWORD=devpassword123
FRONTEND_URL=http://localhost:5173
BACKEND_URL=http://localhost:8080
```

### 3. Start Infrastructure (PostgreSQL, Redis, ChromaDB)
```bash
docker-compose up -d postgres redis
```

**Verify services:**
```bash
docker ps
# Should see: postgres:15 and redis:7
```

### 4. Setup Python AI Service
```bash
cd ai-service

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Create .env for AI service
cat > .env << EOF
GEMINI_API_KEY=your_actual_gemini_key
DATABASE_URL=postgresql://codesage:devpassword123@localhost:5432/codesage
CHROMA_PERSIST_DIR=./chroma_data
EOF

# Start AI service
uvicorn app.main:app --reload --port 8000
```

**Test AI service:**
```bash
curl http://localhost:8000/health
# Should return: {"status": "healthy"}
```

### 5. Setup Backend (in new terminal)
```bash
cd /Users/adii/Builds/AI-Powered-Code-Review-Assistant/backend

# Build (this will also run Flyway migrations)
mvn clean install -DskipTests

# Run backend
mvn spring-boot:run
```

**Test backend:**
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

## 🧪 Testing the API

### 1. Check Services
```bash
# AI Service health
curl http://localhost:8000/ | jq

# Backend health  
curl http://localhost:8080/actuator/health | jq

# Database check
docker exec -it codesage-postgres psql -U codesage -d codesage -c "\dt"
```

### 2. Test Authentication Flow
Since there's no frontend, you'll need to:
1. Navigate to: `http://localhost:8080/oauth2/authorize/github`
2. Authorize with GitHub
3. Get JWT token from callback

### 3. Test Analysis Endpoint (Mock)
```bash
# Test AI service directly
curl -X POST http://localhost:8000/ai/analyze/pr \
  -H "Content-Type: application/json" \
  -d '{
    "repository_id": "test-repo",
    "pr_number": 1,
    "diff": "diff --git a/test.java b/test.java\n+public void badMethod() {\n+  int x = 1;\n+}",
    "files": [],
    "language": "java"
  }' | jq
```

## 🐛 Common Issues

**Issue: Backend won't start**
```bash
# Check if postgres is running
docker ps | grep postgres

# Check logs
docker logs codesage-postgres

# Verify database exists
docker exec -it codesage-postgres psql -U codesage -c "\l"
```

**Issue: AI service fails**
```bash
# Check Gemini API key
echo $GEMINI_API_KEY

# Test directly
python3 -c "import google.generativeai as genai; genai.configure(api_key='YOUR_KEY'); print('OK')"
```

**Issue: Redis connection failed**
```bash
docker logs codesage-redis
docker exec -it codesage-redis redis-cli ping
# Should return: PONG
```

## 📊 What You Can Test

### ✅ Working
1. **Authentication** - GitHub OAuth login flow  
2. **Database** - All tables created via Flyway
3. **AI Analysis** - Direct API calls to analyze code
4. **Caching** - Redis storing PR diffs
5. **WebSocket** - Connect via wscat: `wscat -c ws://localhost:8080/ws`

### ❌ Not Working (Missing Frontend)
1. Repository management UI
2. PR list and details view
3. Real-time suggestion display
4. User dashboard

## 🔜 Next Steps

**Option A: Build Simple Test Frontend**
Create a minimal HTML page to test API

**Option B: Test with Postman/Insomnia**
Import API endpoints and test manually

**Option C: Build Full Frontend**
Implement React app (50+ hours of work)

**Option D: Write Integration Tests**
Test backend + AI service integration

## 📝 API Endpoints Available

**Backend (http://localhost:8080)**
- `POST /oauth2/authorize/github` - Start OAuth login
- `POST /api/v1/auth/refresh` - Refresh JWT token
- `GET /api/v1/repositories` - List user repos (requires auth)
- `POST /api/v1/repositories` - Add repo + webhook
- `POST /api/v1/webhooks/github` - Receive GitHub events
- `GET /api/v1/pull-requests` - List PRs
- `POST /api/v1/pull-requests/{id}/analyze` - Trigger analysis

**AI Service (http://localhost:8000)**
- `GET /health` - Health check
- `POST /ai/analyze/pr` - Analyze PR
- `POST /ai/analyze/file` - Analyze single file
- `POST /ai/patterns/add` - Add code pattern
- `POST /ai/patterns/similar` - Find similar patterns

## 🎯 Recommended Testing Workflow

1. **Start services** (postgres, redis, backend, ai-service)
2. **Test auth** - Login via GitHub OAuth  
3. **Get JWT token** - Save from callback
4. **Test AI directly** - Call `/ai/analyze/pr` with sample diff
5. **Test full flow** - Use Postman to call backend → ai-service

The **MVP backend is ready**, but you'll need a frontend or API client to test the full user experience!
