"""
Analysis Router - API endpoints for code analysis
"""

import logging
from typing import Dict, Any
from fastapi import APIRouter, HTTPException, BackgroundTasks
from pydantic import BaseModel, Field

from app.services.gemini_service import gemini_service
from app.services.rag_service import rag_service

logger = logging.getLogger(__name__)

router = APIRouter()


class AnalyzePRRequest(BaseModel):
    """Request model for PR analysis"""
    repository_id: str = Field(..., description="Repository UUID")
    pr_number: int = Field(..., description="Pull request number")
    diff: str = Field(..., description="Unified diff of the PR")
    files: list[Dict[str, Any]] = Field(default_factory=list, description="Changed files metadata")
    language: str = Field(default="java", description="Primary programming language")


class AnalyzePRResponse(BaseModel):
    """Response model for PR analysis"""
    suggestions: list[Dict[str, Any]] = Field(..., description="List of code review suggestions")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Analysis metadata")


@router.post("/analyze/pr", response_model=AnalyzePRResponse)
async def analyze_pr(request: AnalyzePRRequest, background_tasks: BackgroundTasks):
    """
    Analyze a pull request and generate code review suggestions
    
    This endpoint:
    1. Retrieves similar patterns from RAG
    2. Sends code to Gemini for analysis
    3. Returns structured suggestions
    4. Stores patterns in background
    """
    try:
        logger.info(f"Analyzing PR #{request.pr_number} for repository {request.repository_id}")
        
        # Step 1: Find similar patterns from RAG
        similar_patterns = await rag_service.find_similar_patterns(
            code_snippet=request.diff[:1000],  # Use first 1000 chars for similarity
            repository_id=request.repository_id,
            language=request.language,
            limit=3
        )
        
        # Step 2: Analyze with Gemini
        suggestions = await gemini_service.analyze_code(
            diff=request.diff,
            language=request.language,
            context=f"Repository: {request.repository_id}, PR: {request.pr_number}",
            similar_patterns=similar_patterns
        )
        
        # Step 3: Prepare metadata
        metadata = {
            "files_analyzed": len(request.files),
            "suggestions_count": len(suggestions),
            "similar_patterns_used": len(similar_patterns),
            "language": request.language
        }
        
        # Step 4: Store new patterns in background
        background_tasks.add_task(
            _store_code_patterns,
            request.repository_id,
            request.files,
            request.language
        )
        
        logger.info(f"Analysis complete: {len(suggestions)} suggestions generated")
        
        return AnalyzePRResponse(
            suggestions=suggestions,
            metadata=metadata
        )
        
    except Exception as e:
        logger.error(f"Analysis failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")


@router.post("/analyze/file")
async def analyze_file(
    repository_id: str,
    file_path: str,
    code: str,
    language: str = "java"
):
    """
    Analyze a single file
    
    Useful for real-time analysis as users type
    """
    try:
        logger.info(f"Analyzing file: {file_path}")
        
        # Find similar patterns
        similar_patterns = await rag_service.find_similar_patterns(
            code_snippet=code[:1000],
            repository_id=repository_id,
            language=language,
            limit=3
        )
        
        # Create simple diff format for Gemini
        simplified_diff = f"File: {file_path}\n```{language}\n{code}\n```"
        
        # Analyze
        suggestions = await gemini_service.analyze_code(
            diff=simplified_diff,
            language=language,
            similar_patterns=similar_patterns
        )
        
        return {
            "suggestions": suggestions,
            "file_path": file_path
        }
        
    except Exception as e:
        logger.error(f"File analysis failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def _store_code_patterns(
    repository_id: str,
    files: list[Dict[str, Any]],
    language: str
):
    """Background task to store code patterns"""
    try:
        for file_data in files:
            # Extract relevant code snippets
            # In a real implementation, would parse the file and extract functions/classes
            file_path = file_data.get("filename", "unknown")
            
            # For now, just log
            logger.debug(f"Would store patterns from {file_path}")
            
    except Exception as e:
        logger.error(f"Error storing patterns: {e}")
