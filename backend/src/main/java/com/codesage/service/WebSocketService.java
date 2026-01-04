package com.codesage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket Service - Send real-time updates to connected clients
 * 
 * Message types:
 * - progress: Analysis progress updates (0-100%)
 * - suggestion: New suggestion generated
 * - complete: Analysis completed
 * - error: Analysis failed
 * 
 * Destinations:
 * - /topic/analysis/{analysisId} - Public updates for specific analysis
 * - /queue/user/{userId} - Private user-specific messages
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send progress update
     * 
     * @param analysisId Analysis UUID
     * @param progress   Progress percentage (0-100)
     * @param message    Status message
     */
    public void sendProgressUpdate(UUID analysisId, int progress, String message) {
        Map<String, Object> payload = Map.of(
                "type", "progress",
                "analysisId", analysisId.toString(),
                "progress", progress,
                "message", message,
                "timestamp", System.currentTimeMillis());

        String destination = "/topic/analysis/" + analysisId;
        messagingTemplate.convertAndSend(destination, payload);

        log.debug("Sent progress update: {} - {}%: {}", analysisId, progress, message);
    }

    /**
     * Send new suggestion notification
     * 
     * @param analysisId Analysis UUID
     * @param suggestion Suggestion data
     */
    public void sendSuggestion(UUID analysisId, Map<String, Object> suggestion) {
        Map<String, Object> payload = Map.of(
                "type", "suggestion",
                "analysisId", analysisId.toString(),
                "suggestion", suggestion,
                "timestamp", System.currentTimeMillis());

        String destination = "/topic/analysis/" + analysisId;
        messagingTemplate.convertAndSend(destination, payload);

        log.debug("Sent suggestion notification for analysis: {}", analysisId);
    }

    /**
     * Send analysis completion notification
     * 
     * @param analysisId      Analysis UUID
     * @param suggestionCount Total number of suggestions
     */
    public void sendCompletion(UUID analysisId, int suggestionCount) {
        Map<String, Object> payload = Map.of(
                "type", "complete",
                "analysisId", analysisId.toString(),
                "suggestionCount", suggestionCount,
                "timestamp", System.currentTimeMillis());

        String destination = "/topic/analysis/" + analysisId;
        messagingTemplate.convertAndSend(destination, payload);

        log.info("Sent completion notification for analysis: {} ({} suggestions)",
                analysisId, suggestionCount);
    }

    /**
     * Send error notification
     * 
     * @param analysisId   Analysis UUID
     * @param errorMessage Error message
     */
    public void sendError(UUID analysisId, String errorMessage) {
        Map<String, Object> payload = Map.of(
                "type", "error",
                "analysisId", analysisId.toString(),
                "message", errorMessage,
                "timestamp", System.currentTimeMillis());

        String destination = "/topic/analysis/" + analysisId;
        messagingTemplate.convertAndSend(destination, payload);

        log.error("Sent error notification for analysis: {} - {}", analysisId, errorMessage);
    }

    /**
     * Send custom message to specific user
     * 
     * @param userId  User ID
     * @param message Message payload
     */
    public void sendToUser(String userId, Map<String, Object> message) {
        String destination = "/queue/user/" + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.debug("Sent message to user: {}", userId);
    }
}
