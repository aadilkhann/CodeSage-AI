"""
Unit Tests for Pattern Detector
"""

import pytest
import sys
sys.path.append('/Users/adii/Builds/AI-Powered-Code-Review-Assistant/ai-service')

from app.services.pattern_detector import PatternDetector, PatternMatch


@pytest.fixture
def detector():
    """Create pattern detector instance"""
    return PatternDetector()


def test_detect_java_empty_catch(detector):
    """Test detection of empty catch blocks in Java"""
    code = """
public void method() {
    try {
        riskyOperation();
    } catch (Exception e) {
    }
}
"""
    matches = detector.detect_patterns(code, "java")
    
    # Should detect empty catch
    assert any(m.pattern_type == 'empty_catch' for m in matches)


def test_detect_java_console_logging(detector):
    """Test detection of System.out.println"""
    code = """
public void method() {
    System.out.println("Debug message");
}
"""
    matches = detector.detect_patterns(code, "java")
    
    assert any(m.pattern_type == 'console_logging' for m in matches)


def test_detect_python_bare_except(detector):
    """Test detection of bare except in Python"""
    code = """
try:
    dangerous_operation()
except:
    pass
"""
    matches = detector.detect_patterns(code, "python")
    
    assert any(m.pattern_type == 'bare_except' for m in matches)


def test_detect_python_mutable_default(detector):
    """Test detection of mutable default arguments"""
    code = """
def function(items=[]):
    items.append(1)
    return items
"""
    matches = detector.detect_patterns(code, "python")
    
    assert any(m.pattern_type == 'mutable_default_argument' for m in matches)


def test_detect_javascript_console_log(detector):
    """Test detection of console.log"""
    code = """
function test() {
    console.log('Debug');
}
"""
    matches = detector.detect_patterns(code, "javascript")
    
    assert any(m.pattern_type == 'console_log' for m in matches)


def test_detect_hardcoded_credentials(detector):
    """Test detection of hardcoded credentials"""
    code = """
String password = "secret123";
String apiKey = "ak_1234567890";
"""
    matches = detector.detect_patterns(code, "java")
    
    credential_matches = [m for m in matches if m.pattern_type == 'hardcoded_credentials']
    assert len(credential_matches) >= 1


def test_filter_by_severity(detector):
    """Test filtering by severity"""
    matches = [
        PatternMatch("test1", "info", "msg", 1, "code", 0.9),
        PatternMatch("test2", "minor", "msg", 2, "code", 0.9),
        PatternMatch("test3", "major", "msg", 3, "code", 0.9),
        PatternMatch("test4", "critical", "msg", 4, "code", 0.9),
    ]
    
    filtered = detector.filter_by_severity(matches, "major")
    
    assert len(filtered) == 2  # major and critical
    assert all(m.severity in ["major", "critical"] for m in filtered)


def test_group_by_type(detector):
    """Test grouping by pattern type"""
    matches = [
        PatternMatch("type_a", "info", "msg", 1, "code", 0.9),
        PatternMatch("type_a", "info", "msg", 2, "code", 0.9),
        PatternMatch("type_b", "major", "msg", 3, "code", 0.9),
    ]
    
    grouped = detector.group_by_type(matches)
    
    assert len(grouped) == 2
    assert len(grouped["type_a"]) == 2
    assert len(grouped["type_b"]) == 1


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
