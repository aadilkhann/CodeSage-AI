package com.codesage.service;

import com.codesage.exception.GitHubApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Service for GitHub API integration
 * 
 * Features:
 * - Fetch user repositories from GitHub
 * - Get PR details and diff
 * - Create and delete webhooks
 * - Validate webhook signatures
 * - Circuit breaker for resilience
 * - Automatic retries on failures
 * 
 * Circuit Breaker Strategy:
 * - Configured in application.yml
 * (resilience4j.circuitbreaker.instances.github)
 * - Opens after 50% failure rate in sliding window of 10 requests
 * - Half-open state after 5 seconds
 * - Prevents cascading failures when GitHub API is down
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubService {

    @Value("${app.github.api-url}")
    private String githubApiUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    private final WebClient.Builder webClientBuilder;

    private static final String CIRCUIT_BREAKER_NAME = "github";
    private static final String RETRY_NAME = "github";

    /**
     * Fetch user's repositories from GitHub
     * 
     * @param accessToken GitHub OAuth access token
     * @return List of repository data
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fetchRepositoriesFallback")
    @Retry(name = RETRY_NAME)
    public List<Map<String, Object>> fetchUserRepositories(String accessToken) {
        log.info("Fetching user repositories from GitHub");

        try {
            WebClient webClient = createAuthenticatedClient(accessToken);

            List<Map<String, Object>> repositories = webClient
                    .get()
                    .uri(githubApiUrl + "/user/repos?sort=updated&per_page=100&affiliation=owner,collaborator")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("Successfully fetched {} repositories", repositories != null ? repositories.size() : 0);
            return repositories;

        } catch (WebClientResponseException e) {
            log.error("GitHub API error while fetching repositories: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new GitHubApiException("Failed to fetch repositories from GitHub", e);
        }
    }

    /**
     * Get pull request details
     * 
     * @param accessToken  GitHub OAuth access token
     * @param repoFullName Repository full name (owner/repo)
     * @param prNumber     PR number
     * @return PR data
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPullRequestFallback")
    @Retry(name = RETRY_NAME)
    public Map<String, Object> getPullRequest(String accessToken, String repoFullName, int prNumber) {
        log.info("Fetching PR #{} for repository {}", prNumber, repoFullName);

        try {
            WebClient webClient = createAuthenticatedClient(accessToken);

            Map<String, Object> pr = webClient
                    .get()
                    .uri(githubApiUrl + "/repos/" + repoFullName + "/pulls/" + prNumber)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Successfully fetched PR #{} details", prNumber);
            return pr;

        } catch (WebClientResponseException e) {
            log.error("GitHub API error while fetching PR: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException("Failed to fetch PR #" + prNumber, e);
        }
    }

    /**
     * Get pull request diff
     * 
     * @param accessToken  GitHub OAuth access token
     * @param repoFullName Repository full name (owner/repo)
     * @param prNumber     PR number
     * @return Diff content in unified diff format
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPullRequestDiffFallback")
    @Retry(name = RETRY_NAME)
    public String getPullRequestDiff(String accessToken, String repoFullName, int prNumber) {
        log.info("Fetching diff for PR #{} in repository {}", prNumber, repoFullName);

        try {
            WebClient webClient = createAuthenticatedClient(accessToken);

            String diff = webClient
                    .get()
                    .uri(githubApiUrl + "/repos/" + repoFullName + "/pulls/" + prNumber)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully fetched diff for PR #{} (size: {} bytes)", prNumber,
                    diff != null ? diff.length() : 0);
            return diff;

        } catch (WebClientResponseException e) {
            log.error("GitHub API error while fetching diff: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException("Failed to fetch diff for PR #" + prNumber, e);
        }
    }

    /**
     * Get files changed in a pull request
     * 
     * @param accessToken  GitHub OAuth access token
     * @param repoFullName Repository full name (owner/repo)
     * @param prNumber     PR number
     * @return List of changed files with their details
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    @Retry(name = RETRY_NAME)
    public List<Map<String, Object>> getPullRequestFiles(String accessToken, String repoFullName, int prNumber) {
        log.info("Fetching changed files for PR #{} in repository {}", prNumber, repoFullName);

        try {
            WebClient webClient = createAuthenticatedClient(accessToken);

            List<Map<String, Object>> files = webClient
                    .get()
                    .uri(githubApiUrl + "/repos/" + repoFullName + "/pulls/" + prNumber + "/files")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("Successfully fetched {} changed files for PR #{}", files != null ? files.size() : 0, prNumber);
            return files;

        } catch (WebClientResponseException e) {
            log.error("GitHub API error while fetching PR files: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new GitHubApiException("Failed to fetch files for PR #" + prNumber, e);
        }
    }

    /**
     * Create a webhook for a repository
     * 
     * @param accessToken   GitHub OAuth access token
     * @param repoFullName  Repository full name (owner/repo)
     * @param webhookSecret Secret for verifying webhook signatures
     * @return Webhook data including webhook ID
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Map<String, Object> createWebhook(String accessToken, String repoFullName, String webhookSecret) {
        log.info("Creating webhook for repository {}", repoFullName);

        try {
            WebClient webClient = createAuthenticatedClient(accessToken);

            String webhookUrl = backendUrl + "/api/v1/webhooks/github";

            Map<String, Object> webhookPayload = Map.of(
                    "name", "web",
                    "active", true,
                    "events", List.of("pull_request", "pull_request_review"),
                    "config", Map.of(
                            "url", webhookUrl,
                            "content_type", "json",
                            "secret", webhookSecret,
                            "insecure_ssl", "0"));

            Map<String, Object> webhook = webClient
                    .post()
                    .uri(githubApiUrl + "/repos/" + repoFullName + "/hooks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(webhookPayload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Successfully created webhook for repository {}, webhook ID: {}",
                    repoFullName, webhook != null ? webhook.get("id") : "unknown");
            return webhook;

        } catch (WebClientResponseException e) {
            log.error("GitHub API error while creating webhook: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new GitHubApiException("Failed to create webhook for repository " + repoFullName, e);
        }
    }

    /**
     * Delete a webhook from a repository
     * 
     * @param accessToken  GitHub OAuth access token
     * @param repoFullName Repository full name (owner/repo)
     * @param webhookId    GitHub webhook ID
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public void deleteWebhook(String accessToken, String repoFullName, Long webhookId) {
        log.info("Deleting webhook {} for repository {}", webhookId, repoFullName);

        try {
            WebClient webClient = createAuthenticatedClient(accessToken);

            webClient
                    .delete()
                    .uri(githubApiUrl + "/repos/" + repoFullName + "/hooks/" + webhookId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Successfully deleted webhook {} for repository {}", webhookId, repoFullName);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Webhook {} not found for repository {}, may have been already deleted", webhookId,
                        repoFullName);
            } else {
                log.error("GitHub API error while deleting webhook: {} - {}", e.getStatusCode(),
                        e.getResponseBodyAsString());
                throw new GitHubApiException("Failed to delete webhook " + webhookId, e);
            }
        }
    }

    /**
     * Validate GitHub webhook signature
     * Uses HMAC-SHA256 algorithm
     * 
     * @param payload   The webhook payload body
     * @param signature The signature from X-Hub-Signature-256 header
     * @param secret    The webhook secret
     * @return true if signature is valid
     */
    public boolean validateWebhookSignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("Invalid signature format: {}", signature);
            return false;
        }

        try {
            String expectedSignature = "sha256=" + generateHmacSha256(payload, secret);
            boolean isValid = expectedSignature.equals(signature);

            if (!isValid) {
                log.warn("Webhook signature validation failed");
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private String generateHmacSha256(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * Create authenticated WebClient with GitHub token
     */
    private WebClient createAuthenticatedClient(String accessToken) {
        return webClientBuilder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ==================== Fallback Methods ====================

    /**
     * Fallback method when fetchUserRepositories fails
     */
    private List<Map<String, Object>> fetchRepositoriesFallback(String accessToken, Throwable throwable) {
        log.error("Circuit breaker fallback: Failed to fetch repositories", throwable);
        throw new GitHubApiException("GitHub service is temporarily unavailable. Please try again later.", throwable);
    }

    /**
     * Fallback method when getPullRequest fails
     */
    private Map<String, Object> getPullRequestFallback(String accessToken, String repoFullName, int prNumber,
            Throwable throwable) {
        log.error("Circuit breaker fallback: Failed to fetch PR #{}", prNumber, throwable);
        throw new GitHubApiException("Unable to fetch PR details. GitHub service may be temporarily unavailable.",
                throwable);
    }

    /**
     * Fallback method when getPullRequestDiff fails
     */
    private String getPullRequestDiffFallback(String accessToken, String repoFullName, int prNumber,
            Throwable throwable) {
        log.error("Circuit breaker fallback: Failed to fetch diff for PR #{}", prNumber, throwable);
        throw new GitHubApiException("Unable to fetch PR diff. GitHub service may be temporarily unavailable.",
                throwable);
    }
}
