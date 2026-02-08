"""
Pydantic Models for Request/Response Validation

All API endpoints use these models for:
- Request validation
- Response serialization
- Auto-generated OpenAPI docs
"""

from pydantic import BaseModel, Field, validator
from typing import List, Optional, Dict, Any
from enum import Enum


# ==================== Enums ====================

class SuggestionCategory(str, Enum):
    """Suggestion categories"""
    NAMING = "naming"
    ARCHITECTURE = "architecture"
    PERFORMANCE = "performance"
    SECURITY = "security"
    TESTING = "testing"
    ERROR_HANDLING = "error_handling"
    CODE_STYLE = "code_style"
    DOCUMENTATION = "documentation"
    BEST_PRACTICES = "best_practices"
    GENERAL = "general"


class SuggestionSeverity(str, Enum):
    """Suggestion severity levels"""
    CRITICAL = "critical"
    MAJOR = "major"
    MINOR = "minor"
    INFO = "info"


# ==================== Analysis Requests ====================

class FileInfo(BaseModel):
    """Information about a changed file"""
    filename: str = Field(..., description="File path relative to repository root")
    status: str = Field(..., description="File status: added, modified, removed")
    additions: int = Field(0, description="Number of lines added")
    deletions: int = Field(0, description="Number of lines deleted")
    changes: int = Field(0, description="Total changes")
    patch: Optional[str] = Field(None, description="Diff patch for this file")
    
    class Config:
        schema_extra = {
            "example": {
                "filename": "src/main/java/com/example/Service.java",
                "status": "modified",
                "additions": 15,
                "deletions": 5,
                "changes": 20,
                "patch": "@@ -10,7 +10,8 @@..."
            }
        }


class AnalyzePRRequest(BaseModel):
    """Request to analyze a pull request"""
    repository_id: str = Field(..., description="Repository ID (UUID)")
    pr_number: int = Field(..., gt=0, description="Pull request number")
    diff: str = Field(..., min_length=1, description="Unified diff of PR changes")
    files: List[Dict[str, Any]] = Field(..., description="List of changed files")
    language: str = Field("unknown", description="Primary programming language")
    context: Optional[Dict[str, Any]] = Field(None, description="Additional context")
    
    @validator('diff')
    def validate_diff(cls, v):
        """Ensure diff is not empty"""
        if not v or not v.strip():
            raise ValueError("Diff cannot be empty")
        return v
    
    class Config:
        schema_extra = {
            "example": {
                "repository_id": "123e4567-e89b-12d3-a456-426614174000",
                "pr_number": 42,
                "diff": "diff --git a/src/Main.java...",
                "files": [
                    {
                        "filename": "src/Main.java",
                        "status": "modified",
                        "additions": 10,
                        "deletions": 2
                    }
                ],
                "language": "java"
            }
        }


class AnalyzeFileRequest(BaseModel):
    """Request to analyze a single file"""
    file_path: str = Field(..., description="File path")
    content: str = Field(..., min_length=1, description="File content")
    language: str = Field(..., description="Programming language")
    context: Optional[str] = Field(None, description="Additional context")
    
    class Config:
        schema_extra = {
            "example": {
                "file_path": "src/main/java/Service.java",
                "content": "public class Service { ... }",
                "language": "java",
                "context": "Part of user authentication module"
            }
        }


# ==================== Analysis Responses ====================

class SuggestionResponse(BaseModel):
    """A single code review suggestion"""
    file_path: str = Field(..., description="File path where issue was found")
    line_number: Optional[int] = Field(None, description="Starting line number")
    line_end: Optional[int] = Field(None, description="Ending line number (for multi-line)")
    category: SuggestionCategory = Field(..., description="Suggestion category")
    severity: SuggestionSeverity = Field(..., description="Severity level")
    message: str = Field(..., description="Brief description of the issue")
    explanation: str = Field(..., description="Detailed explanation with context")
    suggested_fix: Optional[str] = Field(None, description="Suggested code fix")
    confidence_score: float = Field(..., ge=0, le=100, description="AI confidence (0-100)")
    
    @validator('line_end')
    def validate_line_end(cls, v, values):
        """Ensure line_end >= line_number"""
        if v is not None and 'line_number' in values and values['line_number'] is not None:
            if v < values['line_number']:
                raise ValueError("line_end must be >= line_number")
        return v
    
    class Config:
        schema_extra = {
            "example": {
                "file_path": "src/main/java/Service.java",
                "line_number": 42,
                "line_end": 45,
                "category": "naming",
                "severity": "minor",
                "message": "Variable name 'x' is not descriptive",
                "explanation": "Based on similar code patterns, variable names should be descriptive. Consider using 'userCount' instead.",
                "suggested_fix": "int userCount = users.size();",
                "confidence_score": 85.5
            }
        }


class AnalysisSummary(BaseModel):
    """Summary of analysis results"""
    total_suggestions: int = Field(..., description="Total number of suggestions")
    by_severity: Dict[str, int] = Field(..., description="Count by severity")
    by_category: Dict[str, int] = Field(..., description="Count by category")
    files_analyzed: int = Field(..., description="Number of files analyzed")
    lines_analyzed: int = Field(..., description="Total lines analyzed")
    
    class Config:
        schema_extra = {
            "example": {
                "total_suggestions": 12,
                "by_severity": {"critical": 1, "major": 3, "minor": 8},
                "by_category": {"naming": 5, "performance": 2, "security": 1},
                "files_analyzed": 5,
                "lines_analyzed": 342
            }
        }


class AnalyzePRResponse(BaseModel):
    """Response from PR analysis"""
    suggestions: List[SuggestionResponse] = Field(..., description="List of suggestions")
    summary: AnalysisSummary = Field(..., description="Analysis summary")
    metadata: Optional[Dict[str, Any]] = Field(None, description="Additional metadata")
    
    class Config:
        schema_extra = {
            "example": {
                "suggestions": [
                    {
                        "file_path": "src/Main.java",
                        "line_number": 10,
                        "category": "security",
                        "severity": "critical",
                        "message": "SQL injection vulnerability",
                        "explanation": "User input is directly concatenated into SQL query...",
                        "confidence_score": 95.0
                    }
                ],
                "summary": {
                    "total_suggestions": 1,
                    "by_severity": {"critical": 1},
                    "by_category": {"security": 1},
                    "files_analyzed": 3,
                    "lines_analyzed": 150
                },
                "metadata": {
                    "analysis_duration_ms": 2500,
                    "gemini_tokens_used": 1200
                }
            }
        }


# ==================== Pattern Management ====================

class AddPatternRequest(BaseModel):
    """Request to add a code pattern to RAG"""
    code: str = Field(..., min_length=1, description="Code snippet")
    language: str = Field(..., description="Programming language")
    category: SuggestionCategory = Field(..., description="Pattern category")
    description: str = Field(..., description="Pattern description")
    tags: List[str] = Field(default_factory=list, description="Search tags")
    metadata: Optional[Dict[str, Any]] = Field(None, description="Additional metadata")
    
    class Config:
        schema_extra = {
            "example": {
                "code": "public static final String API_KEY = \"...\";",
                "language": "java",
                "category": "security",
                "description": "Hardcoded API key - security vulnerability",
                "tags": ["security", "api-key", "credentials"],
                "metadata": {"severity": "critical"}
            }
        }


class AddPatternResponse(BaseModel):
    """Response after adding pattern"""
    pattern_id: str = Field(..., description="Unique pattern ID")
    message: str = Field(..., description="Success message")
    
    class Config:
        schema_extra = {
            "example": {
                "pattern_id": "pattern_abc123",
                "message": "Pattern added successfully"
            }
        }


class SimilarPatternsRequest(BaseModel):
    """Request to find similar patterns"""
    code: str = Field(..., min_length=1, description="Code to search for")
    language: Optional[str] = Field(None, description="Filter by language")
    category: Optional[SuggestionCategory] = Field(None, description="Filter by category")
    limit: int = Field(10, ge=1, le=50, description="Max results to return")
    
    class Config:
        schema_extra = {
            "example": {
                "code": "String password = \"12345\";",
                "language": "java",
                "category": "security",
                "limit": 5
            }
        }


class PatternMatch(BaseModel):
    """A single pattern match result"""
    pattern_id: str = Field(..., description="Pattern ID")
    code: str = Field(..., description="Matched code snippet")
    description: str = Field(..., description="Pattern description")
    similarity_score: float = Field(..., ge=0, le=1, description="Similarity score (0-1)")
    category: SuggestionCategory = Field(..., description="Pattern category")
    language: str = Field(..., description="Programming language")
    
    class Config:
        schema_extra = {
            "example": {
                "pattern_id": "pattern_xyz789",
                "code": "private static String SECRET_KEY = \"...\";",
                "description": "Hardcoded secret key",
                "similarity_score": 0.92,
                "category": "security",
                "language": "java"
            }
        }


class SimilarPatternsResponse(BaseModel):
    """Response with similar patterns"""
    matches: List[PatternMatch] = Field(..., description="Matched patterns")
    total_count: int = Field(..., description="Total matches found")
    
    class Config:
        schema_extra = {
            "example": {
                "matches": [
                    {
                        "pattern_id": "pattern_123",
                        "code": "String apiKey = \"hardcoded\";",
                        "description": "Hardcoded credential",
                        "similarity_score": 0.95,
                        "category": "security",
                        "language": "java"
                    }
                ],
                "total_count": 1
            }
        }


# ==================== Health Check ====================

class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field(..., description="Service status")
    version: str = Field(..., description="Service version")
    gemini_connected: bool = Field(..., description="Gemini API connection status")
    chroma_connected: bool = Field(..., description="ChromaDB connection status")
    
    class Config:
        schema_extra = {
            "example": {
                "status": "healthy",
                "version": "1.0.0",
                "gemini_connected": True,
                "chroma_connected": True
            }
        }


# ==================== Error Responses ====================

class ErrorResponse(BaseModel):
    """Error response"""
    error: str = Field(..., description="Error type")
    message: str = Field(..., description="Error message")
    details: Optional[Dict[str, Any]] = Field(None, description="Additional error details")
    
    class Config:
        schema_extra = {
            "example": {
                "error": "ValidationError",
                "message": "Invalid request parameters",
                "details": {"field": "diff", "issue": "cannot be empty"}
            }
        }
