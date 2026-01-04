"""
Patterns Router - API endpoints for managing code patterns
"""

import logging
from typing import Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.services.rag_service import rag_service

logger = logging.getLogger(__name__)

router = APIRouter()


class StorePatternRequest(BaseModel):
    """Request model for storing a code pattern"""
    code: str = Field(..., description="Code snippet")
    repository_id: str = Field(..., description="Repository UUID")
    file_path: str = Field(..., description="File path in repository")
    language: str = Field(..., description="Programming language")
    category: str = Field(..., description="Pattern category")
    description: str = Field(default="", description="Pattern description")


class FindSimilarRequest(BaseModel):
    """Request model for finding similar patterns"""
    code: str = Field(..., description="Code to find similarities for")
    repository_id: Optional[str] = Field(None, description="Filter by repository")
    language: str = Field(default="java", description="Programming language")
    limit: int = Field(default=5, ge=1, le=20, description="Max results")


@router.post("/patterns/add")
async def add_pattern(request: StorePatternRequest):
    """
    Manually add a code pattern to the vector database
    
    Useful for:
    - Bootstrapping with good examples
    - Adding team coding standards
    - Highlighting exemplary code
    """
    try:
        pattern_id = await rag_service.store_pattern(
            code=request.code,
            repository_id=request.repository_id,
            file_path=request.file_path,
            language=request.language,
            category=request.category,
            description=request.description
        )
        
        return {
            "success": True,
            "pattern_id": pattern_id,
            "message": "Pattern stored successfully"
        }
        
    except Exception as e:
        logger.error(f"Error adding pattern: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/patterns/similar")
async def find_similar(request: FindSimilarRequest):
    """
    Find similar code patterns
    
    Useful for:
    - Consistency checking
    - Finding examples
    - Code search
    """
    try:
        patterns = await rag_service.find_similar_patterns(
            code_snippet=request.code,
            repository_id=request.repository_id or "",
            language=request.language,
            limit=request.limit
        )
        
        return {
            "patterns": patterns,
            "count": len(patterns)
        }
        
    except Exception as e:
        logger.error(f"Error finding similar patterns: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/patterns/stats/{repository_id}")
async def get_repository_stats(repository_id: str):
    """
    Get pattern statistics for a repository
    
    Returns:
    - Total patterns stored
    - Breakdown by category
    """
    try:
        stats = await rag_service.get_repository_stats(repository_id)
        return stats
        
    except Exception as e:
        logger.error(f"Error getting stats: {e}")
        raise HTTPException(status_code=500, detail=str(e))
