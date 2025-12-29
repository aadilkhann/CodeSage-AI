package com.codesage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching operations using Redis
 * 
 * Cache Strategy:
 * Cache keys are organized by prefix:
 * - github:pr:{prId} - PR metadata (TTL: 1 hour)
 * - github:diff:{prId} - PR diff (TTL: 1 hour)
 * - github:user:{userId} - User profile (TTL: 24 hours)
 * - github:repo:{repoId} - Repository info (TTL: 24 hours)
 * - analysis:result:{analysisId} - Analysis results (TTL: 7 days)
 * 
 * Why cache?
 * - Reduce GitHub API calls (rate limit: 5000/hour)
 * - Improve response time
 * - Reduce load on database
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Cache TTLs
    private static final Duration PR_CACHE_TTL = Duration.ofHours(1);
    private static final Duration DIFF_CACHE_TTL = Duration.ofHours(1);
    private static final Duration USER_CACHE_TTL = Duration.ofHours(24);
    private static final Duration REPO_CACHE_TTL = Duration.ofHours(24);
    private static final Duration ANALYSIS_CACHE_TTL = Duration.ofDays(7);

    /**
     * Cache PR metadata
     */
    public void cachePullRequest(String prId, Object prData) {
        String key = "github:pr:" + prId;
        cacheObject(key, prData, PR_CACHE_TTL);
    }

    /**
     * Get cached PR metadata
     */
    public <T> T getCachedPullRequest(String prId, Class<T> type) {
        String key = "github:pr:" + prId;
        return getObject(key, type);
    }

    /**
     * Cache PR diff
     */
    public void cacheDiff(String prId, String diff) {
        String key = "github:diff:" + prId;
        cacheString(key, diff, DIFF_CACHE_TTL);
    }

    /**
     * Get cached PR diff
     */
    public String getCachedDiff(String prId) {
        String key = "github:diff:" + prId;
        return getString(key);
    }

    /**
     * Cache user profile
     */
    public void cacheUser(String userId, Object userData) {
        String key = "github:user:" + userId;
        cacheObject(key, userData, USER_CACHE_TTL);
    }

    /**
     * Get cached user profile
     */
    public <T> T getCachedUser(String userId, Class<T> type) {
        String key = "github:user:" + userId;
        return getObject(key, type);
    }

    /**
     * Cache repository info
     */
    public void cacheRepository(String repoId, Object repoData) {
        String key = "github:repo:" + repoId;
        cacheObject(key, repoData, REPO_CACHE_TTL);
    }

    /**
     * Get cached repository info
     */
    public <T> T getCachedRepository(String repoId, Class<T> type) {
        String key = "github:repo:" + repoId;
        return getObject(key, type);
    }

    /**
     * Cache analysis result
     */
    public void cacheAnalysisResult(String analysisId, Object analysisData) {
        String key = "analysis:result:" + analysisId;
        cacheObject(key, analysisData, ANALYSIS_CACHE_TTL);
    }

    /**
     * Get cached analysis result
     */
    public <T> T getCachedAnalysisResult(String analysisId, Class<T> type) {
        String key = "analysis:result:" + analysisId;
        return getObject(key, type);
    }

    /**
     * Invalidate cache by key
     */
    public void invalidate(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Invalidated cache key: {}", key);
        } catch (Exception e) {
            log.error("Error invalidating cache key: {}", key, e);
        }
    }

    /**
     * Invalidate cache by pattern
     * Example: "github:pr:*" to invalidate all PR caches
     */
    public void invalidatePattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} cache keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error invalidating cache pattern: {}", pattern, e);
        }
    }

    /**
     * Check if key exists in cache
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking cache key existence: {}", key, e);
            return false;
        }
    }

    /**
     * Get remaining TTL for a key
     */
    public Long getTTL(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            return -1L;
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Cache an object (serialized to JSON)
     */
    private void cacheObject(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cached object at key: {} with TTL: {}", key, ttl);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object for caching: {}", key, e);
        } catch (Exception e) {
            log.error("Error caching object: {}", key, e);
        }
    }

    /**
     * Get cached object (deserialized from JSON)
     */
    private <T> T getObject(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Cache hit for key: {}", key);
                return objectMapper.readValue(json, type);
            }
            log.debug("Cache miss for key: {}", key);
            return null;
        } catch (JsonProcessingException e) {
            log.error("Error deserializing cached object: {}", key, e);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving cached object: {}", key, e);
            return null;
        }
    }

    /**
     * Cache a string value
     */
    private void cacheString(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cached string at key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Error caching string: {}", key, e);
        }
    }

    /**
     * Get cached string value
     */
    private String getString(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit for key: {}", key);
            } else {
                log.debug("Cache miss for key: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Error retrieving cached string: {}", key, e);
            return null;
        }
    }
}
