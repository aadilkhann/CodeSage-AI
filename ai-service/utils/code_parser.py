"""
Code Parser Utility - Parse source code files using tree-sitter

Supports:
- Java
- Python  
- JavaScript/TypeScript

Features:
- Extract functions and classes
- Get function signatures
- Identify code structure
"""

from typing import Dict, List, Optional
import tree_sitter
from tree_sitter import Language, Parser, Node
import logging

logger = logging.getLogger(__name__)

# Language queries for extracting code elements
JAVA_QUERY = """
(class_declaration
  name: (identifier) @class.name) @class.def

(method_declaration
  name: (identifier) @method.name
  parameters: (formal_parameters) @method.params) @method.def

(constructor_declaration
  name: (identifier) @construct or.name
  parameters: (formal_parameters) @constructor.params) @constructor.def
"""

PYTHON_QUERY = """
(class_definition
  name: (identifier) @class.name) @class.def

(function_definition
  name: (identifier) @function.name
  parameters: (parameters) @function.params) @function.def
"""

JAVASCRIPT_QUERY = """
(class_declaration
  name: (identifier) @class.name) @class.def

(function_declaration
  name: (identifier) @function.name
  parameters: (formal_parameters) @function.params) @function.def

(method_definition
  name: (property_identifier) @method.name
  parameters: (formal_parameters) @method.params) @method.def
"""


class CodeParser:
    """Parse source code and extract structure"""
    
    def __init__(self):
        """Initialize parsers for supported languages"""
        # TODO: Build tree-sitter languages
        # This requires building the tree-sitter parser libraries:
        # git clone https://github.com/tree-sitter/tree-sitter-java
        # git clone https://github.com/tree-sitter/tree-sitter-python
        # git clone https://github.com/tree-sitter/tree-sitter-javascript
        self.parsers = {}
        self.languages = {}
        
        try:
            # Try to load languages
            # Language.build_library('build/languages.so', [...])
            pass
        except Exception as e:
            logger.warning(f"Tree-sitter languages not built: {e}")
            logger.info("Code parsing will use basic extraction")
    
    def parse_file(self, content: str, language: str) -> Optional[Dict]:
        """
        Parse a file and extract code structure
        
        Args:
            content: File content as string
            language: Language name (java, python, javascript, typescript)
        
        Returns:
            Dict with classes, functions, and structure
        """
        language = language.lower()
        
        if language not in self.parsers:
            logger.warning(f"Parser not available for {language}, using fallback")
            return self._fallback_parse(content, language)
        
        try:
            parser = self.parsers[language]
            tree = parser.parse(bytes(content, 'utf8'))
            
            return {
                'language': language,
                'classes': self._extract_classes(tree.root_node, language),
                'functions': self._extract_functions(tree.root_node, language),
                'imports': self._extract_imports(tree.root_node, language),
            }
        except Exception as e:
            logger.error(f"Parse error: {e}")
            return self._fallback_parse(content, language)
    
    def _extract_classes(self, node: Node, language: str) -> List[Dict]:
        """Extract class definitions"""
        classes = []
        # Recursive search for class nodes
        # Implementation depends on tree-sitter being built
        return classes
    
    def _extract_functions(self, node: Node, language: str) -> List[Dict]:
        """Extract function/method definitions"""
        functions = []
        # Recursive search for function nodes
        # Implementation depends on tree-sitter being built
        return functions
    
    def _extract_imports(self, node: Node, language: str) -> List[str]:
        """Extract import statements"""
        imports = []
        # Extract import statements
        return imports
    
    def _fallback_parse(self, content: str, language: str) -> Dict:
        """
        Fallback parser using regex when tree-sitter unavailable
        
        Not as accurate but provides basic extraction
        """
        import re
        
        result = {
            'language': language,
            'classes': [],
            'functions': [],
            'imports': []
        }
        
        if language == 'java':
            # Extract classes
            class_pattern = r'(?:public\s+)?(?:abstract\s+)?class\s+(\w+)'
            result['classes'] = [
                {'name': m.group(1), 'line': content[:m.start()].count('\n') + 1}
                for m in re.finditer(class_pattern, content)
            ]
            
            # Extract methods
            method_pattern = r'(?:public|private|protected)\s+(?:static\s+)?(?:\w+)\s+(\w+)\s*\([^)]*\)'
            result['functions'] = [
                {'name': m.group(1), 'line': content[:m.start()].count('\n') + 1}
                for m in re.finditer(method_pattern, content)
            ]
            
            # Extract imports
            import_pattern = r'import\s+([\w.]+);'
            result['imports'] = [m.group(1) for m in re.finditer(import_pattern, content)]
        
        elif language == 'python':
            # Extract classes
            class_pattern = r'^class\s+(\w+)'
            result['classes'] = [
                {'name': m.group(1), 'line': content[:m.start()].count('\n') + 1}
                for m in re.finditer(class_pattern, content, re.MULTILINE)
            ]
            
            # Extract functions
            func_pattern = r'^def\s+(\w+)\s*\('
            result['functions'] = [
                {'name': m.group(1), 'line': content[:m.start()].count('\n') + 1}
                for m in re.finditer(func_pattern, content, re.MULTILINE)
            ]
            
            # Extract imports
            import_pattern = r'^(?:from\s+[\w.]+\s+)?import\s+([\w.,\s]+)'
            result['imports'] = [m.group(1) for m in re.finditer(import_pattern, content, re.MULTILINE)]
        
        elif language in ['javascript', 'typescript']:
            # Extract classes
            class_pattern = r'class\s+(\w+)'
            result['classes'] = [
                {'name': m.group(1), 'line': content[:m.start()].count('\n') + 1}
                for m in re.finditer(class_pattern, content)
            ]
            
            # Extract functions
            func_pattern = r'(?:function\s+(\w+)|(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s+)?function|\s+(\w+)\s*\([^)]*\)\s*(?:=>|{))'
            result['functions'] = [
                {'name': next(g for g in m.groups() if g), 'line': content[:m.start()].count('\n') + 1}
                for m in re.finditer(func_pattern, content)
            ]
            
            # Extract imports
            import_pattern = r'import\s+.*?from\s+[\'"]([^\'"]+)[\'"]'
            result['imports'] = [m.group(1) for m in re.finditer(import_pattern, content)]
        
        return result
    
    def get_function_at_line(self, content: str, language: str, line_number: int) -> Optional[Dict]:
        """
        Find which function contains a specific line
        
        Args:
            content: File content
            language: Language name
            line_number: Line number to check
        
        Returns:
            Function info if found
        """
        parsed = self.parse_file(content, language)
        if not parsed:
            return None
        
        # Find function containing this line
        # This is a simplified implementation
        for func in parsed.get('functions', []):
            if func.get('line') <= line_number:
                # TODO: Check if line is within function body
                return func
        
        return None


# Singleton instance
code_parser = CodeParser()
