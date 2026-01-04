package com.codesage.controller;

import com.codesage.dto.ApiResponse;
import com.codesage.dto.PullRequestDTO;
import com.codesage.model.PullRequest;
import com.codesage.model.Repository;
import com.codesage.model.User;
import com.codesage.repository.PullRequestRepository;
import com.codesage.repository.RepositoryRepository;
import com.codesage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Pull Request Management
 * 
 * Endpoints:
 * - GET /api/v1/pull-requests - List PRs with filters
 * - GET /api/v1/pull-requests/{id} - Get PR details
 * - POST /api/v1/pull-requests/{id}/analyze - Trigger manual analysis
 * - GET /api/v1/pull-requests/{id}/analysis - Get analysis results
 */
@RestController
@RequestMapping("/api/v1/pull-requests")
@RequiredArgsConstructor
@Slf4j
public class PullRequestController {

    private final PullRequestRepository pullRequestRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;

    /**
     * List pull requests with optional filters
     * 
     * GET /api/v1/pull-requests?repositoryId=xxx&status=open&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PullRequestDTO>>> listPullRequests(
            @RequestParam(required = false) UUID repositoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Fetching pull requests for user: {}", userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PullRequest> pullRequests;

        if (repositoryId != null) {
            Repository repository = repositoryRepository.findById(repositoryId)
                    .orElseThrow(() -> new RuntimeException("Repository not found"));

            // Verify ownership
            if (!repository.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }

            if (status != null) {
                pullRequests = Page.empty(); // Would use custom query
                // pullRequests = pullRequestRepository.findByRepositoryAndStatus(repository,
                // status, pageable);
            } else {
                pullRequests = pullRequestRepository.findByRepository(repository, pageable);
            }
        } else {
            // Get all PRs for all user's repositories
            List<Repository> userRepositories = repositoryRepository.findByUser(user);
            pullRequests = Page.empty(); // Would need custom query to get all PRs from user's repos
        }

        Page<PullRequestDTO> dtoPage = pullRequests.map(this::convertToDTO);

        return ResponseEntity.ok(ApiResponse.success(dtoPage));
    }

    /**
     * Get pull request details by ID
     * 
     * GET /api/v1/pull-requests/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PullRequestDTO>> getPullRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Fetching pull request {} for user: {}", id, userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PullRequest pullRequest = pullRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pull request not found"));

        // Verify ownership through repository
        if (!pullRequest.getRepository().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(convertToDTO(pullRequest)));
    }

    /**
     * Trigger manual analysis for a PR
     * 
     * POST /api/v1/pull-requests/{id}/analyze
     */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<String>> analyzePullRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Triggering analysis for PR {} by user: {}", id, userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PullRequest pullRequest = pullRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pull request not found"));

        // Verify ownership
        if (!pullRequest.getRepository().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        // TODO: Trigger async analysis
        // analysisService.analyzeAsync(pullRequest);

        return ResponseEntity.accepted()
                .body(ApiResponse.success("Analysis started", "Analysis has been queued"));
    }

    /**
     * Get analysis results for a PR
     * 
     * GET /api/v1/pull-requests/{id}/analysis
     */
    @GetMapping("/{id}/analysis")
    public ResponseEntity<ApiResponse<Object>> getPullRequestAnalysis(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Fetching analysis for PR {} by user: {}", id, userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PullRequest pullRequest = pullRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pull request not found"));

        // Verify ownership
        if (!pullRequest.getRepository().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        // TODO: Fetch analysis and suggestions from database
        // Analysis analysis = analysisRepository.findByPullRequest(pullRequest);
        // List<Suggestion> suggestions = suggestionRepository.findByAnalysis(analysis);

        return ResponseEntity.ok(ApiResponse.success(null, "Analysis results would be returned here"));
    }

    // ==================== Helper Methods ====================

    /**
     * Convert PullRequest entity to DTO
     */
    private PullRequestDTO convertToDTO(PullRequest pullRequest) {
        PullRequestDTO dto = new PullRequestDTO();
        dto.setId(pullRequest.getId());
        dto.setRepositoryId(pullRequest.getRepository().getId());
        dto.setRepositoryFullName(pullRequest.getRepository().getFullName());
        dto.setPrNumber(pullRequest.getPrNumber());
        dto.setTitle(pullRequest.getTitle());
        dto.setDescription(pullRequest.getDescription());
        dto.setAuthor(pullRequest.getAuthor());
        dto.setBaseBranch(pullRequest.getBaseBranch());
        dto.setHeadBranch(pullRequest.getHeadBranch());
        dto.setStatus(pullRequest.getStatus());
        dto.setGithubUrl(pullRequest.getGithubUrl());
        dto.setFilesChanged(pullRequest.getFilesChanged());
        dto.setAdditions(pullRequest.getAdditions());
        dto.setDeletions(pullRequest.getDeletions());
        dto.setCreatedAt(pullRequest.getCreatedAt());
        dto.setUpdatedAt(pullRequest.getUpdatedAt());
        return dto;
    }
}
