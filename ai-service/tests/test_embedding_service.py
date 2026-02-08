"""
Unit Tests for Embedding Service
"""

import pytest
import sys
sys.path.append('/Users/adii/Builds/AI-Powered-Code-Review-Assistant/ai-service')

from app.services.embedding_service import EmbeddingService


@pytest.fixture
def embedding_service():
    """Create embedding service instance for testing"""
    return EmbeddingService(gemini_service=None, cache_service=None)


def test_generate_dummy_embedding(embedding_service):
    """Test dummy embedding generation"""
    text = "test code snippet"
    embedding = embedding_service._generate_dummy_embedding(text)
    
    assert embedding is not None
    assert len(embedding) == 768  # Default dimensions
    assert all(isinstance(x, float) for x in embedding)
    
    # Check deterministic (same input = same output)
    embedding2 = embedding_service._generate_dummy_embedding(text)
    assert embedding == embedding2


def test_cosine_similarity():
    """Test cosine similarity calculation"""
    vec1 = [1.0, 0.0, 0.0]
    vec2 = [1.0, 0.0, 0.0]
    vec3 = [0.0, 1.0, 0.0]
    
    # Same vectors have similarity 1.0
    similarity = EmbeddingService.cosine_similarity(vec1, vec2)
    assert abs(similarity - 1.0) < 0.001
    
    # Orthogonal vectors have similarity 0.0
    similarity = EmbeddingService.cosine_similarity(vec1, vec3)
    assert abs(similarity - 0.0) < 0.001


def test_embed_code_snippet(embedding_service):
    """Test code snippet embedding"""
    code = "public class Test { }"
    language = "java"
    description = "Simple test class"
    
    embedding = embedding_service.embed_code_snippet(code, language, description)
    
    assert embedding is not None
    assert len(embedding) == 768


def test_generate_cache_key(embedding_service):
    """Test cache key generation"""
    text = "test"
    task_type = "retrieval_document"
    
    key1 = embedding_service._generate_cache_key(text, task_type)
    key2 = embedding_service._generate_cache_key(text, task_type)
    
    # Same input produces same key
    assert key1 == key2
    
    # Different task type produces different key
    key3 = embedding_service._generate_cache_key(text, "retrieval_query")
    assert key1 != key3


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
