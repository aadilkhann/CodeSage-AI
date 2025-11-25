package com.codesage.repository;

import com.codesage.model.PullRequest;
import com.codesage.model.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, UUID> {

    /**
     * Find PR by repository and PR number
     * Unique constraint ensures only one result
     */
    Optional<PullRequest> findByRepositoryAndPrNumber(Repository repository, Integer prNumber);

    /**
     * Find all PRs for a repository
     */
    List<PullRequest> findByRepository(Repository repository);

    /**
     * Find PRs by repository with pagination
     */
    Page<PullRequest> findByRepository(Repository repository, Pageable pageable);

    /**
     * Find PRs by status (open, closed, merged)
     */
    List<PullRequest> findByRepositoryAndStatus(Repository repository, String status);

    /**
     * Find open PRs for a repository
     */
    List<PullRequest> findByRepositoryAndStatusOrderByCreatedAtDesc(Repository repository, String status);

    /**
     * Find PRs by author
     */
    List<PullRequest> findByRepositoryAndAuthor(Repository repository, String author);

    /**
     * Count PRs by repository
     */
    long countByRepository(Repository repository);

    /**
     * Count PRs by repository and status
     */
    long countByRepositoryAndStatus(Repository repository, String status);
}
