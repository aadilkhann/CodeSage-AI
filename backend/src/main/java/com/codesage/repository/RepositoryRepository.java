package com.codesage.repository;

import com.codesage.model.Repository;
import com.codesage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RepositoryRepository - Data access for Repository entity
 * 
 * Custom methods for:
 * - Finding repositories by user
 * - Finding active repositories
 * - Looking up by GitHub repo ID
 */
@org.springframework.stereotype.Repository
public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    /**
     * Find repository by GitHub's repository ID
     * Used when receiving webhooks to identify which repo
     */
    Optional<Repository> findByGithubRepoId(Long githubRepoId);

    /**
     * Find all repositories for a user
     * Used in dashboard to list user's repositories
     */
    List<Repository> findByUser(User user);

    /**
     * Find all repositories for a user ID
     */
    List<Repository> findByUserId(UUID userId);

    /**
     * Find only active repositories for a user
     * Filters out paused repositories
     */
    List<Repository> findByUserAndIsActiveTrue(User user);

    /**
     * Find all active repositories
     * Used for background processing
     */
    List<Repository> findByIsActiveTrue();

    /**
     * Check if repository exists by GitHub repo ID
     */
    boolean existsByGithubRepoId(Long githubRepoId);

    /**
     * Count active repositories for a user
     */
    @Query("SELECT COUNT(r) FROM Repository r WHERE r.user.id = :userId AND r.isActive = true")
    long countActiveByUserId(UUID userId);
}
