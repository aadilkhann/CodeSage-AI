package com.codesage.service;

import com.codesage.model.AnalyticsEvent;
import com.codesage.model.Suggestion;
import com.codesage.repository.AnalyticsEventRepository;
import com.codesage.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Suggestion Service - Manages suggestion lifecycle and user feedback
 * 
 * Features:
 * - Update suggestion status (accept/reject)
 * - Store user feedback
 * - Track analytics events
 * - Learn from feedback for RAG improvement
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final AnalyticsEventRepository analyticsEventRepository;

    /**
     * Accept a suggestion
     * 
     * @param suggestionId Suggestion ID
     * @param feedback     Optional user feedback
     * @return Updated suggestion
     */
    @Transactional
    public Suggestion acceptSuggestion(UUID suggestionId, String feedback) {
        log.info("Accepting suggestion: {}", suggestionId);

        Suggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));

        // Update status
        suggestion.setStatus("accepted");
        suggestion.setFeedback(feedback);
        suggestion.setRespondedAt(LocalDateTime.now());

        Suggestion updated = suggestionRepository.save(suggestion);

        // Track analytics
        trackSuggestionEvent(suggestion, "suggestion_accepted", feedback);

        // TODO: Send to RAG service to learn from accepted suggestions
        // This would improve future suggestions based on user preferences

        log.info("Suggestion accepted: {} (category={}, severity={})",
                suggestionId, suggestion.getCategory(), suggestion.getSeverity());

        return updated;
    }

    /**
     * Reject a suggestion
     * 
     * @param suggestionId Suggestion ID
     * @param feedback     Optional user feedback (reason for rejection)
     * @return Updated suggestion
     */
    @Transactional
    public Suggestion rejectSuggestion(UUID suggestionId, String feedback) {
        log.info("Rejecting suggestion: {}", suggestionId);

        Suggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));

        // Update status
        suggestion.setStatus("rejected");
        suggestion.setFeedback(feedback);
        suggestion.setRespondedAt(LocalDateTime.now());

        Suggestion updated = suggestionRepository.save(suggestion);

        // Track analytics
        trackSuggestionEvent(suggestion, "suggestion_rejected", feedback);

        // TODO: Send to RAG service to learn from rejected suggestions
        // This helps filter out unwanted suggestion types

        log.info("Suggestion rejected: {} (category={}, severity={}, feedback={})",
                suggestionId, suggestion.getCategory(), suggestion.getSeverity(),
                feedback != null ? "provided" : "none");

        return updated;
    }

    /**
     * Update suggestion status (generic method)
     * 
     * @param suggestionId Suggestion ID
     * @param status       New status (pending, accepted, rejected)
     * @return Updated suggestion
     */
    @Transactional
    public Suggestion updateSuggestionStatus(UUID suggestionId, String status) {
        log.info("Updating suggestion {} status to {}", suggestionId, status);

        Suggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));

        suggestion.setStatus(status);

        if (!"pending".equals(status)) {
            suggestion.setRespondedAt(LocalDateTime.now());
        }

        return suggestionRepository.save(suggestion);
    }

    /**
     * Store user feedback on a suggestion
     * 
     * @param suggestionId Suggestion ID
     * @param feedback     User feedback text
     * @return Updated suggestion
     */
    @Transactional
    public Suggestion storeFeedback(UUID suggestionId, String feedback) {
        log.info("Storing feedback for suggestion: {}", suggestionId);

        Suggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));

        suggestion.setFeedback(feedback);
        Suggestion updated = suggestionRepository.save(suggestion);

        // Track analytics
        trackSuggestionEvent(suggestion, "feedback_provided", feedback);

        return updated;
    }

    /**
     * Track analytics event for suggestion interaction
     * 
     * @param suggestion The suggestion
     * @param eventType  Event type (e.g., "suggestion_accepted")
     * @param feedback   Additional feedback text
     */
    private void trackSuggestionEvent(Suggestion suggestion, String eventType, String feedback) {
        try {
            AnalyticsEvent event = new AnalyticsEvent();
            event.setUser(suggestion.getAnalysis().getPullRequest().getRepository().getUser());
            event.setRepository(suggestion.getAnalysis().getPullRequest().getRepository());
            event.setEventType(eventType);

            // Build metadata Map
            java.util.Map<String, Object> eventMetadata = new java.util.HashMap<>();
            eventMetadata.put("suggestion_id", suggestion.getId().toString());
            eventMetadata.put("category", suggestion.getCategory());
            eventMetadata.put("severity", suggestion.getSeverity());
            eventMetadata.put("file", suggestion.getFilePath());
            if (feedback != null && !feedback.isEmpty()) {
                eventMetadata.put("feedback", feedback);
            }

            event.setMetadata(eventMetadata);

            analyticsEventRepository.save(event);

            log.debug("Analytics event tracked: {} for suggestion {}", eventType, suggestion.getId());
        } catch (Exception e) {
            log.error("Failed to track analytics event", e);
            // Don't fail the main operation if analytics tracking fails
        }
    }
}
