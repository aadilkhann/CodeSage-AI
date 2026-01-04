"""
RAG Service - Retrieval-Augmented Generation with ChromaDB

Features:
- Store code patterns in vector database
- Semantic similarity search
- Context retrieval for analysis
- Pattern learning from feedback
"""

import logging
from typing import List, Dict, Any, Optional
import chromadb
from chromadb.config import Settings as ChromaSettings
import hashlib

from app.config import settings

logger = logging.getLogger(__name__)


class RAGService:
    """Service for storing and retrieving code patterns using ChromaDB"""
    
    def __init__(self):
        """Initialize ChromaDB client"""
        try:
            # Initialize Chroma client with persistence
            self.client = chromadb.Client(ChromaSettings(
                persist_directory=settings.chroma_persist_dir,
                anonymized_telemetry=False
            ))
            
            # Get or create collection
            self.collection = self.client.get_or_create_collection(
                name=settings.chroma_collection_name,
                metadata={"description": "Code patterns and review history"}
            )
            
            logger.info(f"Initialized ChromaDB collection: {settings.chroma_collection_name}")
            logger.info(f"Collection size: {self.collection.count()} patterns")
            
        except Exception as e:
            logger.error(f"Failed to initialize ChromaDB: {e}")
            raise
    
    async def find_similar_patterns(
        self,
        code_snippet: str,
        repository_id: str,
        language: str = "java",
        limit: int = 5
    ) -> List[Dict[str, Any]]:
        """
        Find similar code patterns from the repository
        
        Args:
            code_snippet: Code to find similarities for
            repository_id: Repository UUID
            language: Programming language
            limit: Max number of results
            
        Returns:
            List of similar patterns with metadata
        """
        try:
            if self.collection.count() == 0:
                logger.debug("No patterns in database yet")
                return []
            
            # Query for similar patterns
            results = self.collection.query(
                query_texts=[code_snippet],
                n_results=min(limit, self.collection.count()),
                where={
                    "$and": [
                        {"repository_id": repository_id},
                        {"language": language}
                    ]
                } if repository_id else {"language": language}
            )
            
            # Format results
            patterns = []
            if results and results['documents'] and len(results['documents']) > 0:
                for i, doc in enumerate(results['documents'][0]):
                    pattern = {
                        "code": doc,
                        "metadata": results['metadatas'][0][i] if results['metadatas'] else {},
                        "distance": results['distances'][0][i] if results['distances'] else 0,
                        "description": results['metadatas'][0][i].get('description', '') if results['metadatas'] else ''
                    }
                    patterns.append(pattern)
            
            logger.info(f"Found {len(patterns)} similar patterns")
            return patterns
            
        except Exception as e:
            logger.error(f"Error finding similar patterns: {e}")
            return []
    
    async def store_pattern(
        self,
        code: str,
        repository_id: str,
        file_path: str,
        language: str,
        category: str,
        description: str = "",
        metadata: Optional[Dict[str, Any]] = None
    ) -> str:
        """
        Store a code pattern in the vector database
        
        Args:
            code: The code snippet
            repository_id: Repository UUID
            file_path: File path in repository
            language: Programming language
            category: Pattern category
            description: Human-readable description
            metadata: Additional metadata
            
        Returns:
            Pattern ID
        """
        try:
            # Generate unique ID
            pattern_id = self._generate_pattern_id(code, repository_id, file_path)
            
            # Prepare metadata
            pattern_metadata = {
                "repository_id": repository_id,
                "file_path": file_path,
                "language": language,
                "category": category,
                "description": description
            }
            
            if metadata:
                pattern_metadata.update(metadata)
            
            # Store in ChromaDB
            self.collection.add(
                documents=[code],
                metadatas=[pattern_metadata],
                ids=[pattern_id]
            )
            
            logger.info(f"Stored pattern {pattern_id} for repository {repository_id}")
            return pattern_id
            
        except Exception as e:
            logger.error(f"Error storing pattern: {e}")
            raise
    
    async def store_accepted_suggestion(
        self,
        suggestion_id: str,
        code_before: str,
        code_after: str,
        repository_id: str,
        file_path: str,
        language: str,
        category: str
    ):
        """
        Store an accepted suggestion as a learning pattern
        
        This creates a pattern that the AI can learn from in future analyses
        """
        try:
            description = f"Accepted {category} improvement"
            
            metadata = {
                "suggestion_id": suggestion_id,
                "type": "accepted_suggestion",
                "code_before": code_before[:500],  # Store snippet for context
                "improvement": category
            }
            
            # Store the improved code as a pattern
            await self.store_pattern(
                code=code_after,
                repository_id=repository_id,
                file_path=file_path,
                language=language,
                category=category,
                description=description,
                metadata=metadata
            )
            
            logger.info(f"Stored accepted suggestion {suggestion_id} as learning pattern")
            
        except Exception as e:
            logger.error(f"Error storing accepted suggestion: {e}")
    
    async def get_repository_stats(self, repository_id: str) -> Dict[str, Any]:
        """Get statistics about stored patterns for a repository"""
        try:
            results = self.collection.get(
                where={"repository_id": repository_id}
            )
            
            total_patterns = len(results['ids']) if results['ids'] else 0
            
            # Count by category
            categories = {}
            if results['metadatas']:
                for metadata in results['metadatas']:
                    category = metadata.get('category', 'unknown')
                    categories[category] = categories.get(category, 0) + 1
            
            return {
                "total_patterns": total_patterns,
                "categories": categories
            }
            
        except Exception as e:
            logger.error(f"Error getting repository stats: {e}")
            return {"total_patterns": 0, "categories": {}}
    
    def _generate_pattern_id(self, code: str, repository_id: str, file_path: str) -> str:
        """Generate unique pattern ID"""
        content = f"{repository_id}:{file_path}:{code}"
        return hashlib.sha256(content.encode()).hexdigest()[:16]


# Global instance
rag_service = RAGService()
