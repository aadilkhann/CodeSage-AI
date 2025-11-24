from fastapi import APIRouter
from datetime import datetime

router = APIRouter()

@router.get("/")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "codesage-ai-service",
        "timestamp": datetime.utcnow().isoformat(),
        "version": "1.0.0"
    }

@router.get("/ready")
async def readiness_check():
    """Readiness check endpoint"""
    # TODO: Add checks for Gemini API, Chroma DB, etc.
    return {
        "status": "ready",
        "checks": {
            "gemini_api": "ok",
            "chroma_db": "ok",
            "database": "ok"
        }
    }

@router.get("/live")
async def liveness_check():
    """Liveness check endpoint"""
    return {
        "status": "alive",
        "timestamp": datetime.utcnow().isoformat()
    }
