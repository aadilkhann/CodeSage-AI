package com.codesage.repository;

import com.codesage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository - Data access layer for User entity
 * 
 * Spring Data JPA automatically implements these methods:
 * - save(User): Insert or update
 * - findById(UUID): Find by primary key
 * - findAll(): Get all users
 * - delete(User): Delete user
 * - count(): Count total users
 * 
 * Custom query methods follow Spring Data naming conventions:
 * - findByX: Find entities where field X equals value
 * - existsByX: Check if entity exists where field X equals value
 * 
 * Why these custom methods?
 * - findByGithubId: Look up user during OAuth callback
 * - existsByGithubId: Check if user already registered
 * - findByUsername: Search users by username
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by GitHub ID
     * Used during OAuth login to check if user exists
     * 
     * @param githubId GitHub's unique user ID
     * @return Optional<User> - present if user exists, empty otherwise
     */
    Optional<User> findByGithubId(Long githubId);

    /**
     * Check if user with GitHub ID exists
     * Faster than findByGithubId when you only need to check existence
     * 
     * @param githubId GitHub's unique user ID
     * @return true if user exists, false otherwise
     */
    boolean existsByGithubId(Long githubId);

    /**
     * Find user by username
     * Used for user search functionality
     * 
     * @param username GitHub username
     * @return Optional<User>
     */
    Optional<User> findByUsername(String username);
}
