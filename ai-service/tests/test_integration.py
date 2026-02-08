"""
Basic Integration Test for PR Analysis Workflow

Tests the complete flow from PR creation to suggestion generation
"""

import pytest
from fastapi.testclient import TestClient
import sys
sys.path.append('/Users/adii/Builds/AI-Powered-Code-Review-Assistant/ai-service')

from app.main import app

client = TestClient(app)


def test_health_check():
    """Test that the health endpoint works"""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"


def test_analyze_pr_endpoint():
    """Test PR analysis endpoint with sample data"""
    payload = {
        "repository_id": "test-repo-123",
        "pr_number": 1,
        "diff": """diff --git a/test.java b/test.java
index 1234567..abcdefg 100644
--- a/test.java
+++ b/test.java
@@ -1,5 +1,6 @@
 public class Test {
     public void method() {
-        System.out.println("old");
+        String password = "hardcoded123";
+        System.out.println("new");
     }
 }""",
        "files": [
            {
                "filename": "test.java",
                "status": "modified",
                "additions": 2,
                "deletions": 1
            }
        ],
        "language": "java"
    }
    
    response = client.post("/ai/analyze/pr", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "suggestions" in data
    assert "summary" in data
    assert isinstance(data["suggestions"], list)


def test_analyze_file_endpoint():
    """Test single file analysis"""
    payload = {
        "file_path": "test.py",
        "content": """
def bad_function():
    password = "secret123"
    print("debug message")
    try:
        dangerous_operation()
    except:
        pass
""",
        "language": "python"
    }
    
    response = client.post("/ai/analyze/file", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "suggestions" in data


def test_add_pattern():
    """Test adding a code pattern"""
    payload = {
        "code": "String apiKey = \"hardcoded\";",
        "language": "java",
        "category": "security",
        "description": "Hardcoded API key example",
        "tags": ["security", "credentials"]
    }
    
    response = client.post("/ai/patterns/add", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "pattern_id" in data


def test_similar_patterns():
    """Test finding similar patterns"""
    payload = {
        "code": "password = \"test123\"",
        "language": "python",
        "limit": 5
    }
    
    response = client.post("/ai/patterns/similar", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "matches" in data
    assert isinstance(data["matches"], list)


def test_validation_error():
    """Test that validation works"""
    # Missing required field 'diff'
    payload = {
        "repository_id": "test",
        "pr_number": 1,
        "files": []
    }
    
    response = client.post("/ai/analyze/pr", json=payload)
    assert response.status_code == 422  # Validation error


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
