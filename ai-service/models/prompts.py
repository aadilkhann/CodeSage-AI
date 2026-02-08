"""
Prompt Templates for Code Analysis

Uses string formatting for dynamic prompt generation based on:
- Programming language
- Code context
- Analysis type
- Previous patterns from RAG
"""

from typing import List, Dict, Optional


class PromptTemplates:
    """Collection of prompt templates for different analysis scenarios"""
    
    # ==================== Base Analysis Prompt ====================
    
    BASE_ANALYSIS_TEMPLATE = """You are an expert code reviewer analyzing a pull request.

**Context:**
- Repository: {repository_name}
- PR #{pr_number}: {pr_title}
- Primary Language: {language}
- Files Changed: {files_count}

**Your Task:**
Analyze the following code changes and provide actionable suggestions for improvement.

**Focus Areas:**
1. Security vulnerabilities
2. Performance issues
3. Code quality and best practices
4. Naming conventions
5. Error handling
6. Testing coverage gaps
7. Architecture concerns

**Code Diff:**
```diff
{diff}
```

**Instructions:**
- Be specific and cite line numbers
- Provide code examples for fixes
- Prioritize by severity (critical, major, minor, info)
- Include confidence score (0-100) for each suggestion
- Focus on meaningful improvements, not nitpicks

**Output Format (JSON):**
Return a JSON array of suggestions, each with:
- file_path: string
- line_number: number (optional)
- line_end: number (optional, for multi-line)
- category: "naming" | "architecture" | "performance" | "security" | "testing" | "error_handling" | "code_style" | "documentation" | "best_practices" | "general"
- severity: "critical" | "major" | "minor" | "info"
- message: brief description
- explanation: detailed context
- suggested_fix: code example (optional)
- confidence_score: 0-100
"""

    # ==================== Language-Specific Prompts ====================
    
    JAVA_SPECIFIC = """
**Java-Specific Checks:**
- Proper use of streams and functional programming
- Exception handling (avoid catching generic Exception)
- Resource management (try-with-resources)
- Null safety (@NonNull, Optional usage)
- Thread safety for concurrent code
- Proper use of collections and generics
- Memory leaks (listeners, caches)
"""

    PYTHON_SPECIFIC = """
**Python-Specific Checks:**
- PEP 8 compliance
- Type hints usage
- Exception handling specificity
- Context managers for resources
- List comprehensions vs loops
- Asyncio best practices (if async code)
- Security (SQL injection, command injection)
"""

    JAVASCRIPT_SPECIFIC = """
**JavaScript/TypeScript-Specific Checks:**
- Async/await vs promises
- Proper error handling (try-catch)
- Type safety (TypeScript)
- Memory leaks (event listeners, closures)
- Security (XSS, injection attacks)
- Modern ES6+ syntax usage
- React best practices (if applicable)
"""

    # ==================== RAG-Enhanced Prompt ====================
    
    RAG_ENHANCED_TEMPLATE = """
**Repository Patterns:**
Based on analysis of this repository's codebase, here are relevant patterns and conventions:

{patterns}

**Apply These Patterns:**
- Ensure new code follows existing repository conventions
- Highlight deviations from established patterns
- Suggest alignment with proven approaches from this codebase
"""

    # ==================== Single File Analysis ====================
    
    FILE_ANALYSIS_TEMPLATE = """You are an expert code reviewer analyzing a single file.

**File:** {file_path}
**Language:** {language}
{context}

**Code:**
```{language}
{content}
```

**Analysis Focus:**
1. Code quality and maintainability
2. Potential bugs or edge cases
3. Performance optimizations
4. Security vulnerabilities
5. Best practices adherence
6. Documentation needs

**Output:** Provide specific, actionable suggestions with line numbers and code examples.
"""

    # ==================== Security-Focused Prompt ====================
    
    SECURITY_FOCUS_TEMPLATE = """
**Security Analysis Priority:**

Critical Security Checks:
1. **Injection Attacks:**
   - SQL Injection
   - Command Injection
   - LDAP Injection
   - XSS vulnerabilities

2. **Authentication & Authorization:**
   - Weak password policies
   - Insecure session management
   - Missing authorization checks
   - Hardcoded credentials

3. **Data Protection:**
   - Sensitive data exposure
   - Insecure cryptography
   - Missing input validation
   - Insufficient logging

4. **Dependencies:**
   - Known vulnerable dependencies
   - Outdated libraries

Flag ALL security issues as CRITICAL severity.
"""

    # ==================== Performance-Focused Prompt ====================
    
    PERFORMANCE_FOCUS_TEMPLATE = """
**Performance Analysis Priority:**

Key Performance Checks:
1. **Database Operations:**
   - N+1 query problems
   - Missing indexes
   - Inefficient queries
   - Lack of caching

2. **Algorithms:**
   - Time complexity issues
   - Unnecessary loops or iterations
   - Inefficient data structures

3. **Resource Management:**
   - Memory leaks
   - Connection pool exhaustion
   - File handle leaks

4. **Concurrency:**
   - Blocking operations on main thread
   - Missing async/parallel processing opportunities
"""

    # ==================== Helper Methods ====================
    
    @staticmethod
    def build_pr_analysis_prompt(
        repository_name: str,
        pr_number: int,
        pr_title: str,
        language: str,
        files_count: int,
        diff: str,
        patterns: Optional[List[Dict]] = None,
        focus: Optional[str] = None
    ) -> str:
        """
        Build complete PR analysis prompt
        
        Args:
            repository_name: Repository full name
            pr_number: PR number
            pr_title: PR title
            language: Primary programming language
            files_count: Number of files changed
            diff: Unified diff
            patterns: RAG patterns (optional)
            focus: Special focus area: 'security' or 'performance' (optional)
        
        Returns:
            Complete prompt string
        """
        prompt = PromptTemplates.BASE_ANALYSIS_TEMPLATE.format(
            repository_name=repository_name,
            pr_number=pr_number,
            pr_title=pr_title or "Untitled",
            language=language.title(),
            files_count=files_count,
            diff=diff
        )
        
        # Add language-specific guidance
        language_lower = language.lower()
        if language_lower == 'java':
            prompt += "\n" + PromptTemplates.JAVA_SPECIFIC
        elif language_lower == 'python':
            prompt += "\n" + PromptTemplates.PYTHON_SPECIFIC
        elif language_lower in ['javascript', 'typescript']:
            prompt += "\n" + PromptTemplates.JAVASCRIPT_SPECIFIC
        
        # Add RAG patterns if available
        if patterns:
            pattern_text = "\n".join([
                f"- {p.get('description', 'Pattern')}: {p.get('code', '')[:100]}..."
                for p in patterns[:5]  # Limit to top 5 patterns
            ])
            prompt += "\n" + PromptTemplates.RAG_ENHANCED_TEMPLATE.format(
                patterns=pattern_text
            )
        
        # Add special focus if requested
        if focus == 'security':
            prompt += "\n" + PromptTemplates.SECURITY_FOCUS_TEMPLATE
        elif focus == 'performance':
            prompt += "\n" + PromptTemplates.PERFORMANCE_FOCUS_TEMPLATE
        
        return prompt
    
    @staticmethod
    def build_file_analysis_prompt(
        file_path: str,
        language: str,
        content: str,
        context: Optional[str] = None
    ) -> str:
        """
        Build single file analysis prompt
        
        Args:
            file_path: File path
            language: Programming language
            content: File content
            context: Additional context (optional)
        
        Returns:
            Complete prompt string
        """
        context_str = f"\n**Context:** {context}" if context else ""
        
        return PromptTemplates.FILE_ANALYSIS_TEMPLATE.format(
            file_path=file_path,
            language=language,
            context=context_str,
            content=content
        )
    
    @staticmethod
    def build_test_suggestion_prompt(
        file_path: str,
        language: str,
        content: str,
        existing_tests: Optional[str] = None
    ) -> str:
        """
        Build prompt for suggesting test cases
        
        Args:
            file_path: File being tested
            language: Programming language
            content: Source code
            existing_tests: Existing test code (optional)
        
        Returns:
            Prompt for test suggestions
        """
        prompt = f"""You are a testing expert. Analyze this code and suggest test cases.

**File:** {file_path}
**Language:** {language}

**Source Code:**
```{language}
{content}
```

"""
        if existing_tests:
            prompt += f"""
**Existing Tests:**
```{language}
{existing_tests}
```

**Task:** Identify gaps in test coverage and suggest additional test cases.
"""
        else:
            prompt += """
**Task:** Suggest comprehensive test cases covering:
1. Happy path scenarios
2. Edge cases
3. Error conditions
4. Boundary values
"""
        
        prompt += """
**Output:** Provide specific test case descriptions with example code."""
        
        return prompt


# Singleton instance
prompt_templates = PromptTemplates()
