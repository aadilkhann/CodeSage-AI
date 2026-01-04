package com.codesage.controller;

import com.codesage.dto.ApiResponse;
import com.codesage.model.PullRequest;
import com.codesage.model.Repository;
import com.codesage.repository.PullRequestRepository;
import com.codesage.repository.RepositoryRepository;
import com.codesage.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook Controller - Receives GitHub webhook events
 * 
 * Endpoints:
 * - POST /api/v1/webhooks/github - Receive GitHub webhook events
 * 
 * Handles events:
 * - pull_request (opened, synchronize, reopened)
 * - pull_request_review
 * 
 * Security:
 * - Validates webhook signature using HMAC-SHA256
 * - Public endpoint (no authentication required)
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final GitHubService gitHubService;

    /**
     * Receive GitHub webhook events
     * 
     * POST /api/v1/webhooks/github
     * 
     * Headers required:
     * - X-Hub-Signature-256: Webhook signature for verification
     * - X-GitHub-Event: Type of event (e.g., "pull_request")
     */
    @PostMapping("/github")
    public ResponseEntity<ApiResponse<Void>> handleGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event") String event,
            @RequestHeader(value = "X-GitHub-Delivery") String deliveryId) {

        log.info("Received GitHub webhook event: {} (delivery: {})", event, deliveryId);

        try {
            // Parse payload
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = parsePayload(payload);

            // Extract repository information
            @SuppressWarnings("unchecked")
            Map<String, Object> repoData = (Map<String, Object>) webhookData.get("repository");
            Long githubRepoId = ((Number) repoData.get("id")).longValue();

            // Find repository in our database
            Repository repository = repositoryRepository.findByGithubRepoId(githubRepoId)
                    .orElse(null);

            if (repository == null) {
                log.warn("Webhook received for unregistered repository: {}", githubRepoId);
                return ResponseEntity.ok(ApiResponse.success(null, "Repository not registered"));
            }

            // Validate webhook signature
            if (!gitHubService.validateWebhookSignature(payload, signature, repository.getWebhookSecret())) {
                log.warn("Invalid webhook signature for repository: {}", repository.getFullName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid signature"));
            }

            // Handle event based on type
            switch (event) {
                case "pull_request":
                    handlePullRequestEvent(webhookData, repository);
                    break;
                case "pull_request_review":
                    handlePullRequestReviewEvent(webhookData, repository);
                    break;
                case "ping":
                    log.info("Ping event received for repository: {}", repository.getFullName());
                    break;
                default:
                    log.info("Unhandled event type: {}", event);
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed"));

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error processing webhook"));
        }
    }

    /**
     * Handle pull_request events
     * Actions: opened, synchronize, reopened, closed
     */
    private void handlePullRequestEvent(Map<String, Object> webhookData, Repository repository) {
        String action = (String) webhookData.get("action");
        log.info("Handling pull_request event with action: {} for repository: {}",
                action, repository.getFullName());

        @SuppressWarnings("unchecked")
        Map<String, Object> prData = (Map<String, Object>) webhookData.get("pull_request");

        Integer prNumber = (Integer) prData.get("number");
        String title = (String) prData.get("title");
        String body = (String) prData.get("body");
        String state = (String) prData.get("state");

        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) prData.get("user");
        String author = (String) userData.get("login");

        @SuppressWarnings("unchecked")
        Map<String, Object> baseData = (Map<String, Object>) prData.get("base");
        String baseBranch = (String) baseData.get("ref");

        @SuppressWarnings("unchecked")
        Map<String, Object> headData = (Map<String, Object>) prData.get("head");
        String headBranch = (String) headData.get("ref");

        String htmlUrl = (String) prData.get("html_url");

        // Check if PR already exists
        PullRequest pullRequest = pullRequestRepository
                .findByRepositoryAndPrNumber(repository, prNumber)
                .orElse(null);

        if (pullRequest == null) {
            // Create new PR
            pullRequest = new PullRequest();
            pullRequest.setRepository(repository);
            pullRequest.setPrNumber(prNumber);
        }

        // Update PR data
        pullRequest.setTitle(title);
        pullRequest.setDescription(body);
        pullRequest.setAuthor(author);
        pullRequest.setBaseBranch(baseBranch);
        pullRequest.setHeadBranch(headBranch);
        pullRequest.setStatus(mapPrStatus(state, action));
        pullRequest.setGithubUrl(htmlUrl);

        pullRequestRepository.save(pullRequest);

        log.info("Saved PR #{} for repository: {}", prNumber, repository.getFullName());

        // Trigger analysis for opened, synchronize, or reopened PRs
        if ("opened".equals(action) || "synchronize".equals(action) || "reopened".equals(action)) {
            if (repository.isAutoAnalyzeEnabled()) {
                log.info("Triggering analysis for PR #{}", prNumber);
                // TODO: Trigger async analysis
                // analysisService.analyzeAsync(pullRequest);
            }
        }
    }

    /**
     * Handle pull_request_review events
     */
    private void handlePullRequestReviewEvent(Map<String, Object> webhookData, Repository repository) {
        String action = (String) webhookData.get("action");
        log.info("Handling pull_request_review event with action: {} for repository: {}",
                action, repository.getFullName());

        // TODO: Handle review events (submitted, dismissed)
        // Can be used for learning from approved PRs
    }

    /**
     * Map GitHub PR state and action to our status
     */
    private String mapPrStatus(String state, String action) {
        if ("closed".equals(state)) {
            // Check if merged (would need additional data from webhook)
            return "closed";
        }
        return "open";
    }

    /**
     * Parse JSON payload
     */
    private Map<String, Object> parsePayload(String payload) {
        // Simple JSON parsing - in production use ObjectMapper
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(payload, Map.class);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse webhook payload", e);
        }
    }
}
