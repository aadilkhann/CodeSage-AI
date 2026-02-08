"""
Diff Analyzer Utility - Parse and analyze unified diff format

Features:
- Parse unified diff format
- Extract changed lines
- Identify affected code blocks
- Map changes to functions/classes
"""

from typing import Dict, List, Tuple, Optional
import re
import logging

logger = logging.getLogger(__name__)


class DiffChunk:
    """Represents a single chunk of changes in a diff"""
    
    def __init__(self, file_path: str, old_start: int, old_count: int, new_start: int, new_count: int):
        self.file_path = file_path
        self.old_start = old_start
        self.old_count = old_count
        self.new_start = new_start
        self.new_count = new_count
        self.added_lines: List[Tuple[int, str]] = []  # (line_number, content)
        self.removed_lines: List[Tuple[int, str]] = []
        self.context_lines: List[Tuple[int, str]] = []
    
    def to_dict(self) -> Dict:
        """Convert to dictionary"""
        return {
            'file_path': self.file_path,
            'old_start': self.old_start,
            'old_count': self.old_count,
            'new_start': self.new_start,
            'new_count': self.new_count,
            'added_lines': [{'line': ln, 'content': content} for ln, content in self.added_lines],
            'removed_lines': [{'line': ln, 'content': content} for ln, content in self.removed_lines],
            'context_lines': [{'line': ln, 'content': content} for ln, content in self.context_lines],
        }


class DiffAnalyzer:
    """Parse and analyze unified diff format"""
    
    def __init__(self):
        # Regex patterns for diff parsing
        self.file_header_pattern = re.compile(r'^diff --git a/(.*) b/(.*)$')
        self.chunk_header_pattern = re.compile(r'^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@')
        self.new_file_pattern = re.compile(r'^\+\+\+ b/(.*)$')
        self.old_file_pattern = re.compile(r'^--- a/(.*)$')
    
    def parse_diff(self, diff_text: str) -> List[DiffChunk]:
        """
        Parse unified diff format
        
        Args:
            diff_text: Unified diff as string
        
        Returns:
            List of DiffChunk objects
        """
        chunks = []
        current_file = None
        current_chunk = None
        old_line = 0
        new_line = 0
        
        for line in diff_text.split('\n'):
            # Check for file header
            file_match = self.new_file_pattern.match(line)
            if file_match:
                current_file = file_match.group(1)
                continue
            
            # Check for chunk header: @@ -10,7 +10,8 @@
            chunk_match = self.chunk_header_pattern.match(line)
            if chunk_match:
                old_start = int(chunk_match.group(1))
                old_count = int(chunk_match.group(2)) if chunk_match.group(2) else 1
                new_start = int(chunk_match.group(3))
                new_count = int(chunk_match.group(4)) if chunk_match.group(4) else 1
                
                if current_chunk:
                    chunks.append(current_chunk)
                
                current_chunk = DiffChunk(current_file or '', old_start, old_count, new_start, new_count)
                old_line = old_start
                new_line = new_start
                continue
            
            if not current_chunk:
                continue
            
            # Parse diff lines
            if line.startswith('+') and not line.startswith('+++'):
                # Added line
                content = line[1:]
                current_chunk.added_lines.append((new_line, content))
                new_line += 1
            elif line.startswith('-') and not line.startswith('---'):
                # Removed line
                content = line[1:]
                current_chunk.removed_lines.append((old_line, content))
                old_line += 1
            elif line.startswith(' '):
                # Context line (unchanged)
                content = line[1:]
                current_chunk.context_lines.append((new_line, content))
                old_line += 1
                new_line += 1
        
        # Add last chunk
        if current_chunk:
            chunks.append(current_chunk)
        
        return chunks
    
    def get_changed_files(self, diff_text: str) -> List[str]:
        """
        Extract list of changed file paths
        
        Args:
            diff_text: Unified diff
        
        Returns:
            List of file paths
        """
        files = set()
        for line in diff_text.split('\n'):
            match = self.new_file_pattern.match(line)
            if match:
                files.add(match.group(1))
        return sorted(list(files))
    
    def get_changed_line_ranges(self, diff_text: str, file_path: str) -> List[Tuple[int, int]]:
        """
        Get line ranges that were changed in a specific file
        
        Args:
            diff_text: Unified diff
            file_path: File to analyze
        
        Returns:
            List of (start_line, end_line) tuples
        """
        chunks = self.parse_diff(diff_text)
        ranges = []
        
        for chunk in chunks:
            if chunk.file_path == file_path:
                if chunk.added_lines:
                    start = min(ln for ln, _ in chunk.added_lines)
                    end = max(ln for ln, _ in chunk.added_lines)
                    ranges.append((start, end))
        
        # Merge overlapping ranges
        if not ranges:
            return []
        
        ranges.sort()
        merged = [ranges[0]]
        
        for start, end in ranges[1:]:
            last_start, last_end = merged[-1]
            if start <= last_end + 1:
                # Overlapping or adjacent, merge
                merged[-1] = (last_start, max(end, last_end))
            else:
                merged.append((start, end))
        
        return merged
    
    def get_diff_stats(self, diff_text: str) -> Dict:
        """
        Get statistics about the diff
        
        Args:
            diff_text: Unified diff
        
        Returns:
            Dict with files_changed, additions, deletions
        """
        chunks = self.parse_diff(diff_text)
        
        files_changed = len(set(chunk.file_path for chunk in chunks))
        total_additions = sum(len(chunk.added_lines) for chunk in chunks)
        total_deletions = sum(len(chunk.removed_lines) for chunk in chunks)
        
        return {
            'files_changed': files_changed,
            'additions': total_additions,
            'deletions': total_deletions,
            'chunks': len(chunks),
        }
    
    def filter_code_files(self, file_paths: List[str]) -> List[str]:
        """
        Filter out non-code files (package-lock.json, node_modules, etc.)
        
        Args:
            file_paths: List of file paths
        
        Returns:
            Filtered list of code files
        """
        # Files/directories to skip
        skip_patterns = [
            'package-lock.json',
            'yarn.lock',
            'pom.xml',
            'build.gradle',
            '.gitignore',
            'node_modules/',
            'target/',
            'build/',
            'dist/',
            '.git/',
            '__pycache__/',
            '.env',
            '.DS_Store',
        ]
        
        # Valid code extensions
        code_extensions = {
            '.java', '.py', '.js', '.ts', '.jsx', '.tsx',
            '.go', '.rs', '.cpp', '.c', '.h', '.cs',
            '.rb', '.php', '.swift', '.kt', '.scala'
        }
        
        filtered = []
        for path in file_paths:
            # Skip if matches skip pattern
            if any(pattern in path for pattern in skip_patterns):
                continue
            
            # Only include if has code extension
            if any(path.endswith(ext) for ext in code_extensions):
                filtered.append(path)
        
        return filtered


# Singleton instance
diff_analyzer = DiffAnalyzer()
