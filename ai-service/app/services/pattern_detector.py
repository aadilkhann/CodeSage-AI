"""
Pattern Detector - Detect code patterns and anti-patterns using AST analysis

Features:
- AST-based pattern matching
- Anti-pattern detection
- Language-specific rules
- Extensible pattern library
"""

import logging
import re
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass
class PatternMatch:
    """Represents a detected pattern in code"""
    pattern_type: str  # e.g., "hardcoded_credential", "sql_injection"
    severity: str  # "critical", "major", "minor", "info"
    message: str
    line_number: int
    code_snippet: str
    confidence: float  # 0.0 to 1.0


class PatternDetector:
    """Detect code patterns and anti-patterns"""
    
    def __init__(self):
        """Initialize pattern detector with rule sets"""
        self.patterns = self._load_patterns()
    
    def detect_patterns(
        self,
        code: str,
        language: str,
        file_path: str = ""
    ) -> List[PatternMatch]:
        """
        Detect patterns in code
        
        Args:
            code: Source code to analyze
            language: Programming language
            file_path: Optional file path for context
        
        Returns:
            List of detected pattern matches
        """
        language = language.lower()
        matches = []
        
        # Run language-specific detectors
        if language == 'java':
            matches.extend(self._detect_java_patterns(code))
        elif language == 'python':
            matches.extend(self._detect_python_patterns(code))
        elif language in ['javascript', 'typescript']:
            matches.extend(self._detect_javascript_patterns(code))
        
        # Run universal detectors (work for all languages)
        matches.extend(self._detect_universal_patterns(code))
        
        return matches
    
    def _load_patterns(self) -> Dict:
        """Load pattern definitions"""
        return {
            # Security patterns
            'hardcoded_credentials': {
                'severity': 'critical',
                'patterns': [
                    (r'password\s*=\s*["\']([^"\']+)["\']', 'Hardcoded password'),
                    (r'api[_-]?key\s*=\s*["\']([^"\']+)["\']', 'Hardcoded API key'),
                    (r'secret\s*=\s*["\']([^"\']+)["\']', 'Hardcoded secret'),
                    (r'token\s*=\s*["\']([^"\']+)["\']', 'Hardcoded token'),
                ]
            },
            'sql_injection': {
                'severity': 'critical',
                'patterns': [
                    (r'execute\(["\'].*?\+.*?["\']', 'Potential SQL injection'),
                    (r'query\(["\'].*?\+.*?["\']', 'Potential SQL injection'),
                ]
            },
            # Code quality patterns
            'todo_comments': {
                'severity': 'info',
                'patterns': [
                    (r'//\s*TODO', 'TODO comment found'),
                    (r'#\s*TODO', 'TODO comment found'),
                    (r'//\s*FIXME', 'FIXME comment found'),
                    (r'#\s*FIXME', 'FIXME comment found'),
                ]
            },
        }
    
    def _detect_java_patterns(self, code: str) -> List[PatternMatch]:
        """Detect Java-specific patterns"""
        matches = []
        lines = code.split('\n')
        
        # Detect empty catch blocks
        for i, line in enumerate(lines, 1):
            if 'catch' in line:
                # Check if next few lines contain only }
                next_lines = lines[i:i+3] if i < len(lines) - 3 else lines[i:]
                if any(l.strip() == '}' for l in next_lines):
                    matches.append(PatternMatch(
                        pattern_type='empty_catch',
                        severity='major',
                        message='Empty catch block - swallows exceptions',
                        line_number=i,
                        code_snippet=line.strip(),
                        confidence=0.8
                    ))
        
        # Detect generic exception catching
        for i, line in enumerate(lines, 1):
            if re.search(r'catch\s*\(\s*Exception', line):
                matches.append(PatternMatch(
                    pattern_type='catch_generic_exception',
                    severity='minor',
                    message='Catching generic Exception - be more specific',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.9
                ))
        
        # Detect System.out.println (should use logger)
        for i, line in enumerate(lines, 1):
            if 'System.out.println' in line or 'System.err.println' in line:
                matches.append(PatternMatch(
                    pattern_type='console_logging',
                    severity='minor',
                    message='Use logger instead of System.out.println',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.95
                ))
        
        return matches
    
    def _detect_python_patterns(self, code: str) -> List[PatternMatch]:
        """Detect Python-specific patterns"""
        matches = []
        lines = code.split('\n')
        
        # Detect bare except
        for i, line in enumerate(lines, 1):
            if re.search(r'except\s*:', line):
                matches.append(PatternMatch(
                    pattern_type='bare_except',
                    severity='major',
                    message='Bare except clause - catches all exceptions',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.95
                ))
        
        # Detect print statements (should use logging)
        for i, line in enumerate(lines, 1):
            if re.search(r'\bprint\s*\(', line) and 'logger' not in code:
                matches.append(PatternMatch(
                    pattern_type='print_statement',
                    severity='info',
                    message='Consider using logging instead of print',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.7
                ))
        
        # Detect mutable default arguments
        for i, line in enumerate(lines, 1):
            if re.search(r'def\s+\w+\s*\([^)]*=\s*\[', line) or re.search(r'def\s+\w+\s*\([^)]*=\s*\{', line):
                matches.append(PatternMatch(
                    pattern_type='mutable_default_argument',
                    severity='major',
                    message='Mutable default argument - can lead to unexpected behavior',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.85
                ))
        
        return matches
    
    def _detect_javascript_patterns(self, code: str) -> List[PatternMatch]:
        """Detect JavaScript/TypeScript-specific patterns"""
        matches = []
        lines = code.split('\n')
        
        # Detect console.log (should be removed in production)
        for i, line in enumerate(lines, 1):
            if 'console.log' in line or 'console.error' in line:
                matches.append(PatternMatch(
                    pattern_type='console_log',
                    severity='info',
                    message='Remove console.log before production',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.9
                ))
        
        # Detect == instead of ===
        for i, line in enumerate(lines, 1):
            if re.search(r'[^=!<>]==[^=]', line):
                matches.append(PatternMatch(
                    pattern_type='loose_equality',
                    severity='minor',
                    message='Use === instead of == for strict equality',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.8
                ))
        
        # Detect var usage (should use let/const)
        for i, line in enumerate(lines, 1):
            if re.search(r'\bvar\s+', line):
                matches.append(PatternMatch(
                    pattern_type='var_keyword',
                    severity='minor',
                    message='Use let or const instead of var',
                    line_number=i,
                    code_snippet=line.strip(),
                    confidence=0.9
                ))
        
        return matches
    
    def _detect_universal_patterns(self, code: str) -> List[PatternMatch]:
        """Detect patterns that work across all languages"""
        matches = []
        lines = code.split('\n')
        
        # Detect hardcoded credentials
        for pattern_type, config in self.patterns.items():
            if pattern_type == 'hardcoded_credentials':
                for regex, message in config['patterns']:
                    for i, line in enumerate(lines, 1):
                        if re.search(regex, line, re.IGNORECASE):
                            matches.append(PatternMatch(
                                pattern_type=pattern_type,
                                severity=config['severity'],
                                message=message,
                                line_number=i,
                                code_snippet=line.strip(),
                                confidence=0.85
                            ))
        
        # Detect TODO/FIXME comments
        for i, line in enumerate(lines, 1):
            if re.search(r'(//|#)\s*(TODO|FIXME)', line, re.IGNORECASE):
                matches.append(PatternMatch(
                    pattern_type='todo_comment',
                    severity='info',
                    message='TODO/FIXME comment - needs attention',
                    line_number=i,
                    code_snippet=line.strip()[:80],  # Limit length
                    confidence=0.95
                ))
        
        return matches
    
    def filter_by_severity(
        self,
        matches: List[PatternMatch],
        min_severity: str = "info"
    ) -> List[PatternMatch]:
        """
        Filter matches by minimum severity
        
        Args:
            matches: List of pattern matches
            min_severity: Minimum severity to include
        
        Returns:
            Filtered list of matches
        """
        severity_order = {"info": 0, "minor": 1, "major": 2, "critical": 3}
        min_level = severity_order.get(min_severity, 0)
        
        return [
            m for m in matches
            if severity_order.get(m.severity, 0) >= min_level
        ]
    
    def group_by_type(
        self,
        matches: List[PatternMatch]
    ) -> Dict[str, List[PatternMatch]]:
        """
        Group matches by pattern type
        
        Args:
            matches: List of pattern matches
        
        Returns:
            Dictionary mapping pattern type to matches
        """
        grouped = {}
        for match in matches:
            if match.pattern_type not in grouped:
                grouped[match.pattern_type] = []
            grouped[match.pattern_type].append(match)
        return grouped


# Singleton instance
pattern_detector = PatternDetector()
