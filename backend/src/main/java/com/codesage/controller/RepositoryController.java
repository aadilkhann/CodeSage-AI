package com.codesage.controller;

import com.codesage.dto.AddRepositoryRequest;
import com.codesage.dto.ApiResponse;
import com.codesage.dto.RepositoryDTO;
import com.codesage.model.Repository;
import com.codesage.model.User;
import com.codesage.repository.RepositoryRepository;
import com.codesage.repository.UserRepository;
import com.codesage.security.JwtTokenProvider;
import com.codesage.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Repository Management
 * 
 * Endpoints:
 * - GET /api/v1/repositories - List user's repositories
 * - POST /api/v1/repositories - Add new repository with webhook
 * - GET /api/v1/repositories/{id} - Get repository details
 * - PUT /api/v1/repositories/{id} - Update repository settings
 * - DELETE /api/v1/repositories/{id} - Remove repository and webhook
 * - POST /api/v1/repositories/{id}/sync - Sync with GitHub
 */
@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
@Slf4j
public class RepositoryController {

    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final GitHubService gitHubService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * List all repositories for the authenticated user
     * 
     * GET /api/v1/repositories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryDTO>>> listRepositories(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Fetching repositories for user: {}", userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Repository> repositories = repositoryRepository.findByUser(user);

        List<RepositoryDTO> repositoryDTOs = repositories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(repositoryDTOs));
    }

    /**
     * Add a new repository and create GitHub webhook
     * 
     * POST /api/v1/repositories
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryDTO>> addRepository(
            @Valid @RequestBody AddRepositoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Adding repository {} for user: {}", request.getFullName(), userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if repository already exists
        if (repositoryRepository.existsByGithubRepoId(request.getGithubRepoId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Repository already exists"));
        }

        // Generate webhook secret
        String webhookSecret = generateWebhookSecret();

        // Create webhook on GitHub
        Map<String, Object> webhook = gitHubService.createWebhook(
                user.getAccessTokenHash(),
                request.getFullName(),
                webhookSecret);

        Long webhookId = ((Number) webhook.get("id")).longValue();

        // Create repository entity
        Repository repository = new Repository();
        repository.setUser(user);
        repository.setGithubRepoId(request.getGithubRepoId());
        repository.setFullName(request.getFullName());
        repository.setDescription(request.getDescription());
        repository.setLanguage(request.getLanguage());
        repository.setWebhookId(webhookId);
        repository.setWebhookSecret(webhookSecret);
        repository.setIsActive(true);

        Repository savedRepository = repositoryRepository.save(repository);

        log.info("Successfully added repository {} with webhook ID: {}",
                savedRepository.getFullName(), webhookId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToDTO(savedRepository)));
    }

    /**
     * Get repository details by ID
     * 
     * GET /api/v1/repositories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepositoryDTO>> getRepository(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Fetching repository {} for user: {}", id, userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        // Verify ownership
        if (!repository.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(convertToDTO(repository)));
    }

    /**
     * Update repository settings
     * 
     * PUT /api/v1/repositories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RepositoryDTO>> updateRepository(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating repository {} for user: {}", id, userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        // Verify ownership
        if (!repository.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        // Update settings
        if (updates.containsKey("isActive")) {
            repository.setIsActive((Boolean) updates.get("isActive"));
        }
        if (updates.containsKey("settings")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) updates.get("settings");
            repository.setSettings(settings);
        }

        Repository updatedRepository = repositoryRepository.save(repository);

        log.info("Successfully updated repository {}", id);

        return ResponseEntity.ok(ApiResponse.success(convertToDTO(updatedRepository)));
    }

    /**
     * Delete repository and remove GitHub webhook
     * 
     * DELETE /api/v1/repositories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Deleting repository {} for user: {}", id, userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        // Verify ownership
        if (!repository.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        // Delete webhook from GitHub
        if (repository.getWebhookId() != null) {
            try {
                gitHubService.deleteWebhook(
                        user.getAccessTokenHash(),
                        repository.getFullName(),
                        repository.getWebhookId());
            } catch (Exception e) {
                log.warn("Failed to delete webhook from GitHub: {}", e.getMessage());
                // Continue with repository deletion even if webhook deletion fails
            }
        }

        // Delete repository from database
        repositoryRepository.delete(repository);

        log.info("Successfully deleted repository {}", id);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Sync repository with GitHub
     * Fetches latest information from GitHub API
     * 
     * POST /api/v1/repositories/{id}/sync
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<ApiResponse<RepositoryDTO>> syncRepository(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Syncing repository {} for user: {}", id, userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        // Verify ownership
        if (!repository.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        // Fetch latest info from GitHub would go here
        // For now, just return current state
        // TODO: Implement GitHub API call to fetch repository metadata

        return ResponseEntity.ok(ApiResponse.success(convertToDTO(repository)));
    }

    // ==================== Helper Methods ====================

    /**
     * Convert Repository entity to DTO
     */
    private RepositoryDTO convertToDTO(Repository repository) {
        RepositoryDTO dto = new RepositoryDTO();
        dto.setId(repository.getId());
        dto.setGithubRepoId(repository.getGithubRepoId());
        dto.setFullName(repository.getFullName());
        dto.setDescription(repository.getDescription());
        dto.setLanguage(repository.getLanguage());
        dto.setIsActive(repository.getIsActive());
        dto.setSettings(repository.getSettings());
        dto.setCreatedAt(repository.getCreatedAt());
        dto.setUpdatedAt(repository.getUpdatedAt());
        return dto;
    }

    /**
     * Generate secure random webhook secret
     */
    private String generateWebhookSecret() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
