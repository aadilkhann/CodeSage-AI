"""
Models package initialization
"""

from .schemas import (
    # Enums
    SuggestionCategory,
    SuggestionSeverity,
    # Requests
    AnalyzePRRequest,
    AnalyzeFileRequest,
    AddPatternRequest,
    SimilarPatternsRequest,
    # Responses
    SuggestionResponse,
    AnalysisSummary,
    AnalyzePRResponse,
    AddPatternResponse,
    PatternMatch,
    SimilarPatternsResponse,
    HealthResponse,
    ErrorResponse,
)

from .prompts import PromptTemplates, prompt_templates

__all__ = [
    # Enums
    "SuggestionCategory",
    "SuggestionSeverity",
    # Requests
    "AnalyzePRRequest",
    "AnalyzeFileRequest",
    "AddPatternRequest",
    "SimilarPatternsRequest",
    # Responses
    "SuggestionResponse",
    "AnalysisSummary",
    "AnalyzePRResponse",
    "AddPatternResponse",
    "PatternMatch",
    "SimilarPatternsResponse",
    "HealthResponse",
    "ErrorResponse",
    # Prompts
    "PromptTemplates",
    "prompt_templates",
]
