#!/bin/bash

# CodeSage AI - Quick Start Script
# This script helps you get started quickly

set -e

echo "🚀 CodeSage AI - Quick Start"
echo "============================"
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "📝 Creating .env file from template..."
    cp .env.example .env
    echo "⚠️  Please edit .env and add your API keys:"
    echo "   - GEMINI_API_KEY (get from https://aistudio.google.com/app/apikey)"
    echo "   - GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET"
    echo "   - Generate JWT_SECRET and GITHUB_WEBHOOK_SECRET"
    echo ""
    read -p "Press Enter when you've updated .env..."
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Start infrastructure services
echo "🐘 Starting PostgreSQL and Redis..."
docker compose up -d postgres redis

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service health
echo "🔍 Checking service health..."
docker compose ps

echo ""
echo "✅ Infrastructure services are running!"
echo ""
echo "📚 Next steps:"
echo "   1. Backend: cd backend && ./mvnw spring-boot:run"
echo "   2. AI Service: cd ai-service && python -m venv venv && source venv/bin/activate && pip install -r requirements.txt && uvicorn app.main:app --reload"
echo "   3. Frontend: cd frontend && npm install && npm run dev"
echo ""
echo "   Or run everything with Docker:"
echo "   docker compose up --build"
echo ""
echo "🌐 Access points:"
echo "   - Frontend: http://localhost:5173"
echo "   - Backend: http://localhost:8080"
echo "   - AI Service: http://localhost:8000/docs"
echo "   - Grafana: http://localhost:3000"
echo ""
