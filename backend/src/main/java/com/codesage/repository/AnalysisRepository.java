package com.codesage.repository;

import com.codesage.model.Analysis;
import com.codesage.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    /**
     * Find all analyses for a PR, ordered by most recent first
     */
    List<Analysis> findByPullRequestOrderByCreatedAtDesc(PullRequest pullRequest);

    /**
     * Find the most recent analysis for a PR
     */
    Optional<Analysis> findFirstByPullRequestOrderByCreatedAtDesc(PullRequest pullRequest);

    /**
     * Find analyses by status
     */
    List<Analysis> findByStatus(String status);

    /**
     * Find pending or processing analyses (for monitoring stuck analyses)
     */
    @Query("SELECT a FROM Analysis a WHERE a.status IN ('pending', 'processing') AND a.createdAt < :before")
    List<Analysis> findStuckAnalyses(LocalDateTime before);

    /**
     * Count analyses by status
     */
    long countByStatus(String status);

    /**
     * Get average analysis duration
     */
    @Query("SELECT AVG(a.durationMs) FROM Analysis a WHERE a.status = 'completed' AND a.durationMs IS NOT NULL")
    Double getAverageAnalysisDuration();
}
