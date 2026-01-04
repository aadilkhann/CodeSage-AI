#!/bin/bash

# CodeSage AI - Start Script
# This script starts all services in the correct order

set -e  # Exit on error

echo "🚀 Starting CodeSage AI MVP..."

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${RED}❌ Error: .env file not found${NC}"
    echo "Please copy .env.example to .env and configure it first:"
    echo "  cp .env.example .env"
    echo "  nano .env"
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Check required env vars
REQUIRED_VARS=("GEMINI_API_KEY" "GITHUB_CLIENT_ID" "GITHUB_CLIENT_SECRET" "JWT_SECRET")
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo -e "${RED}❌ Error: $var not set in .env${NC}"
        exit 1
    fi
done

echo -e "${GREEN}✅ Environment variables loaded${NC}"

# Step 1: Start Docker services
echo -e "\n${YELLOW}📦 Starting Docker services (PostgreSQL, Redis)...${NC}"
docker-compose up -d postgres redis

# Wait for PostgreSQL
echo "⏳ Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec codesage-postgres pg_isready -U codesage > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PostgreSQL is ready${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ PostgreSQL failed to start${NC}"
        exit 1
    fi
    sleep 1
done

# Wait for Redis
echo "⏳ Waiting for Redis to be ready..."
for i in {1..10}; do
    if docker exec codesage-redis redis-cli ping > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Redis is ready${NC}"
        break
    fi
    if [ $i -eq 10 ]; then
        echo -e "${RED}❌ Redis failed to start${NC}"
        exit 1
    fi
    sleep 1
done

# Step 2: Start AI Service
echo -e "\n${YELLOW}🤖 Starting AI Service (FastAPI)...${NC}"
cd ai-service

if [ ! -d "venv" ]; then
    echo "Creating Python virtual environment..."
    python3 -m venv venv
fi

source venv/bin/activate

if [ ! -f "venv/.deps_installed" ]; then
    echo "Installing Python dependencies..."
    pip install -q -r requirements.txt
    touch venv/.deps_installed
fi

# Create .env for AI service
cat > .env << EOF
GEMINI_API_KEY=${GEMINI_API_KEY}
DATABASE_URL=postgresql://codesage:${DB_PASSWORD}@localhost:5432/codesage
CHROMA_PERSIST_DIR=./chroma_data
ENVIRONMENT=development
LOG_LEVEL=INFO
EOF

# Start AI service in background
echo "Starting AI service on port 8000..."
nohup uvicorn app.main:app --host 0.0.0.0 --port 8000 > ../logs/ai-service.log 2>&1 &
AI_PID=$!
echo $AI_PID > ../logs/ai-service.pid

# Wait for AI service to be ready
echo "⏳ Waiting for AI service to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8000/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ AI Service is ready (PID: $AI_PID)${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ AI Service failed to start${NC}"
        echo "Check logs: tail -f logs/ai-service.log"
        exit 1
    fi
    sleep 1
done

cd ..

# Step 3: Start Backend
echo -e "\n${YELLOW}☕ Starting Backend (Spring Boot)...${NC}"
cd backend

# Build only if not already built
if [ ! -d "target" ]; then
    echo "Building backend..."
    mvn clean install -DskipTests -q
fi

# Start backend in background
echo "Starting backend on port 8080..."
mkdir -p ../logs
nohup mvn spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > ../logs/backend.pid

# Wait for backend to be ready
echo "⏳ Waiting for backend to be ready..."
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Backend is ready (PID: $BACKEND_PID)${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}❌ Backend failed to start${NC}"
        echo "Check logs: tail -f logs/backend.log"
        exit 1
    fi
    sleep 2
done

cd ..

# Success!
echo -e "\n${GREEN}🎉 All services started successfully!${NC}"
echo ""
echo "📊 Service Status:"
echo "  • PostgreSQL: http://localhost:5432 (user: codesage, db: codesage)"
echo "  • Redis:      http://localhost:6379"
echo "  • AI Service: http://localhost:8000 (docs: /docs)"
echo "  • Backend:    http://localhost:8080 (swagger: /swagger-ui.html)"
echo ""
echo "📝 Logs:"
echo "  • AI Service: tail -f logs/ai-service.log"
echo "  • Backend:    tail -f logs/backend.log"
echo ""
echo "🧪 Quick Tests:"
echo "  curl http://localhost:8000/health"
echo "  curl http://localhost:8080/actuator/health"
echo ""
echo "🛑 To stop all services:"
echo "  ./stop.sh"
