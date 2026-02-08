"""
Embedding Service - Generate and cache embeddings for code patterns

Uses Google's Gemini API for text embeddings to enable RAG (Retrieval-Augmented Generation)

Features:
- Generate embeddings for code snippets
- Batch processing for efficiency
- Caching layer to avoid redundant API calls
- Integration with ChromaDB for vector storage
"""

import logging
from typing import List, Dict, Optional
import hashlib
import json
from functools import lru_cache

logger = logging.getLogger(__name__)


class EmbeddingService:
    """Service for generating and caching code embeddings"""
    
    def __init__(self, gemini_service=None, cache_service=None):
        """
        Initialize embedding service
        
        Args:
            gemini_service: GeminiService instance for API calls
            cache_service: CacheService for caching embeddings
        """
        self.gemini_service = gemini_service
        self.cache_service = cache_service
        self.embedding_model = "models/embedding-001"  # Gemini embedding model
    
    def generate_embedding(self, text: str, task_type: str = "retrieval_document") -> Optional[List[float]]:
        """
        Generate embedding for a single text
        
        Args:
            text: Text to embed (code snippet, description, etc.)
            task_type: Task type for embedding optimization
                - "retrieval_document": For storing in vector DB
                - "retrieval_query": For searching in vector DB
        
        Returns:
            List of floats representing the embedding vector
        """
        if not text or not text.strip():
            logger.warning("Empty text provided for embedding")
            return None
        
        # Check cache first
        cache_key = self._generate_cache_key(text, task_type)
        if self.cache_service:
            cached = self.cache_service.get_embedding(cache_key)
            if cached:
                logger.debug(f"Cache hit for embedding: {cache_key[:16]}...")
                return cached
        
        try:
            # Call Gemini API for embedding
            # Note: Actual implementation depends on gemini_service interface
            if self.gemini_service:
                embedding = self.gemini_service.generate_embedding(
                    text=text,
                    model=self.embedding_model,
                    task_type=task_type
                )
            else:
                # Fallback: Generate a dummy embedding for testing
                # In production, this should raise an error
                logger.warning("No gemini_service provided, using dummy embedding")
                embedding = self._generate_dummy_embedding(text)
            
            # Cache the result
            if self.cache_service and embedding:
                self.cache_service.set_embedding(cache_key, embedding, ttl=86400)  # 24h cache
            
            return embedding
        
        except Exception as e:
            logger.error(f"Failed to generate embedding: {e}")
            return None
    
    def generate_embeddings_batch(
        self,
        texts: List[str],
        task_type: str = "retrieval_document"
    ) -> List[Optional[List[float]]]:
        """
        Generate embeddings for multiple texts in batch
        
        Batch processing is more efficient than calling API repeatedly.
        
        Args:
            texts: List of texts to embed
            task_type: Task type for all embeddings
        
        Returns:
            List of embedding vectors (same order as input)
        """
        if not texts:
            return []
        
        embeddings = []
        uncached_indices = []
        uncached_texts = []
        
        # Check cache for all texts
        for i, text in enumerate(texts):
            if not text or not text.strip():
                embeddings.append(None)
                continue
            
            cache_key = self._generate_cache_key(text, task_type)
            if self.cache_service:
                cached = self.cache_service.get_embedding(cache_key)
                if cached:
                    embeddings.append(cached)
                    continue
            
            # Not in cache, add to batch
            embeddings.append(None)  # Placeholder
            uncached_indices.append(i)
            uncached_texts.append(text)
        
        # Generate embeddings for uncached texts in batch
        if uncached_texts:
            try:
                if self.gemini_service:
                    batch_embeddings = self.gemini_service.generate_embeddings_batch(
                        texts=uncached_texts,
                        model=self.embedding_model,
                        task_type=task_type
                    )
                else:
                    # Fallback for testing
                    batch_embeddings = [self._generate_dummy_embedding(t) for t in uncached_texts]
                
                # Fill in embeddings and cache
                for idx, embedding in zip(uncached_indices, batch_embeddings):
                    embeddings[idx] = embedding
                    
                    if self.cache_service and embedding:
                        cache_key = self._generate_cache_key(texts[idx], task_type)
                        self.cache_service.set_embedding(cache_key, embedding, ttl=86400)
            
            except Exception as e:
                logger.error(f"Batch embedding generation failed: {e}")
                # Fill remaining with None
                for idx in uncached_indices:
                    if embeddings[idx] is None:
                        embeddings[idx] = None
        
        return embeddings
    
    def embed_code_snippet(
        self,
        code: str,
        language: str,
        description: Optional[str] = None
    ) -> Optional[List[float]]:
        """
        Generate embedding for a code snippet
        
        Combines code and description for better semantic representation.
        
        Args:
            code: Source code
            language: Programming language
            description: Optional description/explanation
        
        Returns:
            Embedding vector
        """
        # Build combined text for embedding
        text_parts = [f"Language: {language}", f"Code:\n{code}"]
        
        if description:
            text_parts.append(f"Description: {description}")
        
        combined_text = "\n\n".join(text_parts)
        
        return self.generate_embedding(combined_text, task_type="retrieval_document")
    
    def embed_search_query(self, query: str) -> Optional[List[float]]:
        """
        Generate embedding for a search query
        
        Uses "retrieval_query" task type for better search performance.
        
        Args:
            query: Search query text
        
        Returns:
            Embedding vector optimized for retrieval
        """
        return self.generate_embedding(query, task_type="retrieval_query")
    
    def _generate_cache_key(self, text: str, task_type: str) -> str:
        """
        Generate cache key for embedding
        
        Uses SHA256 hash of text + task_type to ensure uniqueness.
        
        Args:
            text: Input text
            task_type: Task type
        
        Returns:
            Cache key string
        """
        content = f"{task_type}:{text}"
        return hashlib.sha256(content.encode('utf-8')).hexdigest()
    
    def _generate_dummy_embedding(self, text: str, dimensions: int = 768) -> List[float]:
        """
        Generate a deterministic dummy embedding for testing
        
        Uses hash of text to generate consistent values.
        
        Args:
            text: Input text
            dimensions: Embedding dimensions
        
        Returns:
            List of floats (dummy embedding)
        """
        # Use hash to generate deterministic values
        hash_val = int(hashlib.md5(text.encode()).hexdigest(), 16)
        
        # Generate pseudo-random but deterministic embedding
        import random
        random.seed(hash_val)
        
        embedding = [random.uniform(-1, 1) for _ in range(dimensions)]
        
        # Normalize to unit length (common for embeddings)
        magnitude = sum(x**2 for x in embedding) ** 0.5
        if magnitude > 0:
            embedding = [x / magnitude for x in embedding]
        
        return embedding
    
    @staticmethod
    def cosine_similarity(vec1: List[float], vec2: List[float]) -> float:
        """
        Calculate cosine similarity between two embedding vectors
        
        Args:
            vec1: First embedding vector
            vec2: Second embedding vector
        
        Returns:
            Similarity score between -1 and 1 (higher = more similar)
        """
        if not vec1 or not vec2 or len(vec1) != len(vec2):
            return 0.0
        
        dot_product = sum(a * b for a, b in zip(vec1, vec2))
        magnitude1 = sum(a ** 2 for a in vec1) ** 0.5
        magnitude2 = sum(b ** 2 for b in vec2) ** 0.5
        
        if magnitude1 == 0 or magnitude2 == 0:
            return 0.0
        
        return dot_product / (magnitude1 * magnitude2)


# Singleton instance (will be initialized with dependencies in main.py)
embedding_service = EmbeddingService()
