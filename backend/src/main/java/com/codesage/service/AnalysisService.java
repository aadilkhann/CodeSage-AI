package com.codesage.service;

import com.codesage.model.Analysis;
import com.codesage.model.PullRequest;
import com.codesage.model.Suggestion;
import com.codesage.repository.AnalysisRepository;
import com.codesage.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Analysis Service - Orchestrates the complete PR analysis workflow
 * 
 * Workflow:
 * 1. Create analysis record (status: pending)
 * 2. Fetch PR diff from GitHub (with caching)
 * 3. Send to AI service for analysis
 * 4. Process AI response and extract suggestions
 * 5. Save suggestions to database
 * 6. Send WebSocket notifications
 * 7. Mark analysis as complete
 * 
 * Features:
 * - Async processing (non-blocking)
 * - Progress updates via WebSocket
 * - Error handling with retry
 * - Result caching
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final SuggestionRepository suggestionRepository;
    private final GitHubService gitHubService;
    private final CacheService cacheService;
    private final WebSocketService webSocketService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    @Value("${app.ai-service.timeout:120000}")
    private int aiServiceTimeout;

    /**
     * Trigger async analysis for a PR
     * Returns immediately after creating analysis record
     * 
     * @param pullRequest PR to analyze
     * @param accessToken GitHub access token for API calls
     * @return Created analysis entity
     */
    @Transactional
    public Analysis triggerAnalysis(PullRequest pullRequest, String accessToken) {
        log.info("Triggering analysis for PR #{} in repository {}",
                pullRequest.getPrNumber(), pullRequest.getRepository().getFullName());

        // Create analysis record
        Analysis analysis = new Analysis();
        analysis.setPullRequest(pullRequest);
        analysis.setStatus("pending");

        Analysis savedAnalysis = analysisRepository.save(analysis);

        // Trigger async processing
        analyzeAsync(savedAnalysis.getId(), accessToken);

        return savedAnalysis;
    }

    /**
     * Async analysis execution
     * Runs in separate thread pool
     */
    @Async("analysisExecutor")
    @Transactional
    public void analyzeAsync(java.util.UUID analysisId, String accessToken) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Analysis not found"));

        try {
            log.info("Starting async analysis: {}", analysisId);
            analysis.markAsStarted();
            analysisRepository.save(analysis);

            // Send progress update
            sendProgressUpdate(analysis, 10, "Fetching PR diff from GitHub");

            // Step 1: Fetch PR diff
            String diff = fetchPRDiff(analysis, accessToken);

            if (diff == null || diff.isEmpty()) {
                throw new RuntimeException("Empty diff - nothing to analyze");
            }

            // Send progress update
            sendProgressUpdate(analysis, 30, "Preparing code for AI analysis");

            // Step 2: Get PR files for context
            List<Map<String, Object>> files = gitHubService.getPullRequestFiles(
                    accessToken,
                    analysis.getPullRequest().getRepository().getFullName(),
                    analysis.getPullRequest().getPrNumber());

            analysis.setFilesAnalyzed(files.size());

            // Send progress update
            sendProgressUpdate(analysis, 50, "Analyzing code with AI");

            // Step 3: Call AI service for analysis
            List<Map<String, Object>> suggestions = callAIService(analysis, diff, files);

            // Send progress update
            sendProgressUpdate(analysis, 80, "Processing suggestions");

            // Step 4: Save suggestions
            saveSuggestions(analysis, suggestions);

            // Step 5: Mark as completed
            analysis.markAsCompleted();
            analysisRepository.save(analysis);

            // Cache results
            cacheService.cacheAnalysisResult(analysisId.toString(), analysis);

            // Send completion notification
            sendCompletionNotification(analysis, suggestions.size());

            log.info("Analysis completed successfully: {} ({} suggestions, {} ms)",
                    analysisId, suggestions.size(), analysis.getDurationMs());

        } catch (Exception e) {
            log.error("Analysis failed: {}", analysisId, e);
            analysis.markAsFailed(e.getMessage());
            analysisRepository.save(analysis);

            // Send error notification
            sendErrorNotification(analysis, e.getMessage());
        }
    }

    /**
     * Fetch PR diff with caching
     */
    private String fetchPRDiff(Analysis analysis, String accessToken) {
        String prId = analysis.getPullRequest().getId().toString();

        // Try cache first
        String cachedDiff = cacheService.getCachedDiff(prId);
        if (cachedDiff != null) {
            log.info("Using cached diff for PR #{}", analysis.getPullRequest().getPrNumber());
            return cachedDiff;
        }

        // Fetch from GitHub
        String diff = gitHubService.getPullRequestDiff(
                accessToken,
                analysis.getPullRequest().getRepository().getFullName(),
                analysis.getPullRequest().getPrNumber());

        // Cache for future use
        if (diff != null) {
            cacheService.cacheDiff(prId, diff);
        }

        return diff;
    }

    /**
     * Call AI service for code analysis
     */
    private List<Map<String, Object>> callAIService(Analysis analysis, String diff, List<Map<String, Object>> files) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            // Prepare request payload
            Map<String, Object> request = Map.of(
                    "repository_id", analysis.getPullRequest().getRepository().getId().toString(),
                    "pr_number", analysis.getPullRequest().getPrNumber(),
                    "diff", diff,
                    "files", files,
                    "language", analysis.getPullRequest().getRepository().getLanguage() != null
                            ? analysis.getPullRequest().getRepository().getLanguage()
                            : "unknown");

            // Make API call
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient
                    .post()
                    .uri("/ai/analyze/pr")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("suggestions")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> suggestionsList = (List<Map<String, Object>>) response.get("suggestions");
                return suggestionsList;
            }

            return List.of();

        } catch (Exception e) {
            log.error("AI service call failed", e);
            // For now, return empty list if AI service is unavailable
            // TODO: Implement fallback/retry logic
            return List.of();
        }
    }

    /**
     * Save suggestions to database
     */
    @Transactional
    private void saveSuggestions(Analysis analysis, List<Map<String, Object>> suggestionData) {
        for (Map<String, Object> data : suggestionData) {
            Suggestion suggestion = new Suggestion();
            suggestion.setAnalysis(analysis);
            suggestion.setFilePath((String) data.get("file_path"));
            suggestion.setLineNumber(getInteger(data, "line_number"));
            suggestion.setLineEnd(getInteger(data, "line_end"));
            suggestion.setCategory((String) data.get("category"));
            suggestion.setSeverity((String) data.get("severity"));
            suggestion.setMessage((String) data.get("message"));
            suggestion.setExplanation((String) data.get("explanation"));
            suggestion.setSuggestedFix((String) data.get("suggested_fix"));

            if (data.containsKey("confidence_score")) {
                suggestion.setConfidenceScore(new BigDecimal(data.get("confidence_score").toString()));
            }

            suggestion.setStatus("pending");

            suggestionRepository.save(suggestion);

            // Send real-time suggestion notification
            sendSuggestionNotification(analysis, suggestion);
        }
    }

    /**
     * Helper to safely get integer from map
     */
    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof Number)
            return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    // ==================== WebSocket Notifications ====================

    private void sendProgressUpdate(Analysis analysis, int progress, String message) {
        webSocketService.sendProgressUpdate(
                analysis.getId(),
                progress,
                message);
    }

    private void sendSuggestionNotification(Analysis analysis, Suggestion suggestion) {
        webSocketService.sendSuggestion(
                analysis.getId(),
                convertSuggestionToMap(suggestion));
    }

    private void sendCompletionNotification(Analysis analysis, int suggestionCount) {
        webSocketService.sendCompletion(
                analysis.getId(),
                suggestionCount);
    }

    private void sendErrorNotification(Analysis analysis, String errorMessage) {
        webSocketService.sendError(
                analysis.getId(),
                errorMessage);
    }

    private Map<String, Object> convertSuggestionToMap(Suggestion suggestion) {
        return Map.of(
                "id", suggestion.getId().toString(),
                "filePath", suggestion.getFilePath(),
                "lineNumber", suggestion.getLineNumber() != null ? suggestion.getLineNumber() : 0,
                "category", suggestion.getCategory() != null ? suggestion.getCategory() : "general",
                "severity", suggestion.getSeverity() != null ? suggestion.getSeverity() : "minor",
                "message", suggestion.getMessage());
    }
}
