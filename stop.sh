#!/bin/bash

# CodeSage AI - Stop Script

echo "🛑 Stopping CodeSage AI services..."

# Stop backend
if [ -f logs/backend.pid ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        echo "Stopping backend (PID: $BACKEND_PID)..."
        kill $BACKEND_PID
        rm logs/backend.pid
    fi
fi

# Stop AI service
if [ -f logs/ai-service.pid ]; then
    AI_PID=$(cat logs/ai-service.pid)
    if kill -0 $AI_PID 2>/dev/null; then
        echo "Stopping AI service (PID: $AI_PID)..."
        kill $AI_PID
        rm logs/ai-service.pid
    fi
fi

# Stop Docker services
echo "Stopping Docker services..."
docker-compose down

echo "✅ All services stopped"
