"""
Gemini Service - Google Gemini API integration for code analysis

Features:
- Code review with Gemini 2.5 Flash
- Prompt engineering for code analysis
- Rate limiting and retry logic
- Structured output parsing
"""

import logging
import time
from typing import List, Dict, Any, Optional
import google.generativeai as genai

from app.config import settings

logger = logging.getLogger(__name__)


class GeminiService:
    """Service for interacting with Google Gemini AI"""
    
    def __init__(self):
        """Initialize Gemini service"""
        genai.configure(api_key=settings.gemini_api_key)
        
        self.model = genai.GenerativeModel(
            model_name=settings.gemini_model,
            generation_config={
                "temperature": settings.gemini_temperature,
                "max_output_tokens": settings.gemini_max_output_tokens,
            }
        )
        
        self.request_count = 0
        self.last_request_time = 0
        
        logger.info(f"Initialized Gemini service with model: {settings.gemini_model}")
    
    async def analyze_code(
        self,
        diff: str,
        language: str = "java",
        context: Optional[str] = None,
        similar_patterns: Optional[List[Dict[str, Any]]] = None
    ) -> List[Dict[str, Any]]:
        """
        Analyze code changes and generate suggestions
        
        Args:
            diff: Unified diff of the changes
            language: Programming language
            context: Additional context about the repository
            similar_patterns: Similar code patterns from RAG
            
        Returns:
            List of suggestions with structured data
        """
        try:
            # Build prompt
            prompt = self._build_analysis_prompt(diff, language, context, similar_patterns)
            
            # Rate limiting
            await self._rate_limit()
            
            # Call Gemini API
            logger.info(f"Analyzing {language} code with Gemini...")
            response = self.model.generate_content(prompt)
            
            # Parse response
            suggestions = self._parse_suggestions(response.text)
            
            logger.info(f"Generated {len(suggestions)} suggestions")
            return suggestions
            
        except Exception as e:
            logger.error(f"Gemini analysis failed: {e}")
            raise
    
    def _build_analysis_prompt(
        self,
        diff: str,
        language: str,
        context: Optional[str],
        similar_patterns: Optional[List[Dict[str, Any]]]
    ) -> str:
        """Build the analysis prompt for Gemini"""
        
        prompt = f"""You are an expert code reviewer analyzing a pull request.

**Task:** Review the following {language} code changes and provide actionable suggestions.

**Code Changes (Unified Diff):**
```diff
{diff}
```
"""
        
        # Add context if available
        if context:
            prompt += f"\n**Repository Context:**\n{context}\n"
        
        # Add similar patterns from RAG
        if similar_patterns and len(similar_patterns) > 0:
            prompt += "\n**Similar Code Patterns from this Repository:**\n"
            for i, pattern in enumerate(similar_patterns[:3], 1):
                prompt += f"\n{i}. {pattern.get('description', 'Pattern')}\n"
                prompt += f"```{language}\n{pattern.get('code', '')}\n```\n"
        
        # Add instructions
        prompt += """
**Your Task:**
1. Analyze the code changes for potential issues
2. Consider: naming conventions, architecture, performance, security, error handling, testing
3. Focus on actionable improvements
4. Be specific and provide examples

**Output Format:**
For each suggestion, provide a JSON object with these fields:
- file_path: The file being reviewed
- line_number: Starting line number (from diff)
- line_end: Ending line number (optional, for multi-line)
- category: One of [naming, architecture, performance, security, testing, error_handling, documentation, style]
- severity: One of [critical, moderate, minor]
- message: Brief description of the issue (max 100 chars)
- explanation: Detailed explanation with context
- suggested_fix: Proposed code fix (if applicable)
- confidence_score: Your confidence (0-100)

**IMPORTANT:**
- Return ONLY valid JSON array
- Skip obvious or trivial suggestions
- Focus on high-confidence (>70) suggestions
- Consider the repository's existing patterns

**JSON Output:**
```json
[
  {
    "file_path": "src/main/java/Example.java",
    "line_number": 42,
    "category": "naming",
    "severity": "minor",
    "message": "Variable name 'x' is not descriptive",
    "explanation": "Based on similar code patterns, use descriptive names like 'userCount'",
    "suggested_fix": "int userCount = users.size();",
    "confidence_score": 85
  }
]
```
"""
        
        return prompt
    
    def _parse_suggestions(self, response_text: str) -> List[Dict[str, Any]]:
        """Parse Gemini response and extract suggestions"""
        import json
        import re
        
        try:
            # Extract JSON from markdown code blocks
            json_match = re.search(r'```json\n(.*?)\n```', response_text, re.DOTALL)
            if json_match:
                json_text = json_match.group(1)
            else:
                # Try to find JSON array directly
                json_match = re.search(r'\[\s*\{.*?\}\s*\]', response_text, re.DOTALL)
                if json_match:
                    json_text = json_match.group(0)
                else:
                    logger.warning("No JSON found in response")
                    return []
            
            # Parse JSON
            suggestions = json.loads(json_text)
            
            # Validate and clean suggestions
            validated = []
            for suggestion in suggestions:
                if self._validate_suggestion(suggestion):
                    validated.append(suggestion)
                else:
                    logger.warning(f"Invalid suggestion format: {suggestion}")
            
            return validated
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON: {e}")
            logger.debug( f"Response text: {response_text[:500]}")
            return []
        except Exception as e:
            logger.error(f"Error parsing suggestions: {e}")
            return []
    
    def _validate_suggestion(self, suggestion: Dict[str, Any]) -> bool:
        """Validate suggestion has required fields"""
        required_fields = ["file_path", "category", "severity", "message"]
        
        for field in required_fields:
            if field not in suggestion:
                return False
        
        # Validate category
        valid_categories = [
            "naming", "architecture", "performance", "security",
            "testing", "error_handling", "documentation", "style"
        ]
        if suggestion["category"] not in valid_categories:
            suggestion["category"] = "general"
        
        # Validate severity
        valid_severities = ["critical", "moderate", "minor"]
        if suggestion["severity"] not in valid_severities:
            suggestion["severity"] = "minor"
        
        # Set defaults
        suggestion.setdefault("line_number", 0)
        suggestion.setdefault("explanation", "")
        suggestion.setdefault("suggested_fix", "")
        suggestion.setdefault("confidence_score", 75)
        
        return True
    
    async def _rate_limit(self):
        """Simple rate limiting"""
        current_time = time.time()
        
        # Ensure minimum time between requests (1 second)
        if self.last_request_time > 0:
            time_since_last = current_time - self.last_request_time
            if time_since_last < 1.0:
                await self._sleep(1.0 - time_since_last)
        
        self.last_request_time = time.time()
        self.request_count += 1
    
    async def _sleep(self, seconds: float):
        """Async sleep"""
        import asyncio
        await asyncio.sleep(seconds)


# Global instance
gemini_service = GeminiService()
