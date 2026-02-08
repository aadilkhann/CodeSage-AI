package com.codesage.controller;

import com.codesage.dto.ApiResponse;
import com.codesage.dto.SuggestionFeedbackRequest;
import com.codesage.model.Analysis;
import com.codesage.model.Suggestion;
import com.codesage.repository.AnalysisRepository;
import com.codesage.repository.SuggestionRepository;
import com.codesage.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Analysis Controller - Endpoints for analysis status and suggestions
 * 
 * Endpoints:
 * - GET /api/analysis/{id}/status - Get analysis progress
 * - GET /api/analysis/{id}/suggestions - Get all suggestions
 * - POST /api/suggestions/{id}/accept - Accept a suggestion
 * - POST /api/suggestions/{id}/reject - Reject a suggestion
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final AnalysisRepository analysisRepository;
    private final SuggestionRepository suggestionRepository;
    private final SuggestionService suggestionService;

    /**
     * Get analysis status and progress
     * 
     * @param id Analysis ID
     * @return Analysis details with status, progress, metadata
     */
    @GetMapping("/analysis/{id}/status")
    public ResponseEntity<ApiResponse<AnalysisStatusResponse>> getAnalysisStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal OAuth2User principal) {

        log.info("Getting analysis status: {}", id);

        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Analysis not found"));

        // Verify ownership through PR -> Repository -> User
        String githubId = principal.getAttribute("id").toString();
        if (!analysis.getPullRequest().getRepository().getUser().getGithubId().equals(githubId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        AnalysisStatusResponse response = new AnalysisStatusResponse(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getProgress(),
                analysis.getProgressMessage(),
                analysis.getStartedAt(),
                analysis.getCompletedAt(),
                analysis.getFilesAnalyzed(),
                analysis.getDurationMs(),
                analysis.getErrorMessage(),
                suggestionRepository.countByAnalysisId(id));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all suggestions for an analysis
     * 
     * @param id       Analysis ID
     * @param status   Optional status filter (pending, accepted, rejected)
     * @param severity Optional severity filter (critical, major, minor, info)
     * @return List of suggestions
     */
    @GetMapping("/analysis/{id}/suggestions")
    public ResponseEntity<ApiResponse<List<Suggestion>>> getAnalysisSuggestions(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @AuthenticationPrincipal OAuth2User principal) {

        log.info("Getting suggestions for analysis: {} (status={}, severity={})", id, status, severity);

        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Analysis not found"));

        // Verify ownership
        String githubId = principal.getAttribute("id").toString();
        if (!analysis.getPullRequest().getRepository().getUser().getGithubId().equals(githubId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        List<Suggestion> suggestions = suggestionRepository.findByAnalysisId(id);

        // Apply filters
        if (status != null && !status.isEmpty()) {
            suggestions = suggestions.stream()
                    .filter(s -> status.equalsIgnoreCase(s.getStatus()))
                    .toList();
        }

        if (severity != null && !severity.isEmpty()) {
            suggestions = suggestions.stream()
                    .filter(s -> severity.equalsIgnoreCase(s.getSeverity()))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    /**
     * Accept a suggestion
     * 
     * @param id      Suggestion ID
     * @param request Optional feedback
     * @return Updated suggestion
     */
    @PostMapping("/suggestions/{id}/accept")
    public ResponseEntity<ApiResponse<Suggestion>> acceptSuggestion(
            @PathVariable UUID id,
            @RequestBody(required = false) SuggestionFeedbackRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        log.info("Accepting suggestion: {}", id);

        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));

        // Verify ownership
        String githubId = principal.getAttribute("id").toString();
        if (!suggestion.getAnalysis().getPullRequest().getRepository().getUser().getGithubId().equals(githubId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        String feedback = request != null ? request.getFeedback() : null;
        Suggestion updated = suggestionService.acceptSuggestion(id, feedback);

        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * Reject a suggestion
     * 
     * @param id      Suggestion ID
     * @param request Optional feedback (reason for rejection)
     * @return Updated suggestion
     */
    @PostMapping("/suggestions/{id}/reject")
    public ResponseEntity<ApiResponse<Suggestion>> rejectSuggestion(
            @PathVariable UUID id,
            @RequestBody(required = false) SuggestionFeedbackRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        log.info("Rejecting suggestion: {}", id);

        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));

        // Verify ownership
        String githubId = principal.getAttribute("id").toString();
        if (!suggestion.getAnalysis().getPullRequest().getRepository().getUser().getGithubId().equals(githubId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        String feedback = request != null ? request.getFeedback() : null;
        Suggestion updated = suggestionService.rejectSuggestion(id, feedback);

        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    // ==================== Response DTOs ====================

    /**
     * Analysis status response DTO
     */
    public record AnalysisStatusResponse(
            UUID id,
            String status,
            Integer progress,
            String progressMessage,
            java.time.LocalDateTime startedAt,
            java.time.LocalDateTime completedAt,
            Integer filesAnalyzed,
            Long durationMs,
            String errorMessage,
            long suggestionCount) {
    }
}
