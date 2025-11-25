package com.codesage.repository;

import com.codesage.model.CodePattern;
import com.codesage.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface CodePatternRepository extends JpaRepository<CodePattern, UUID> {

    /**
     * Find patterns by repository and type
     */
    List<CodePattern> findByRepositoryAndPatternType(Repository repository, String patternType);

    /**
     * Find patterns by repository and language
     */
    List<CodePattern> findByRepositoryAndLanguage(Repository repository, String language);

    /**
     * Find pattern by embedding ID
     */
    Optional<CodePattern> findByEmbeddingId(String embeddingId);

    /**
     * Find most frequent patterns for a repository
     */
    @Query("SELECT cp FROM CodePattern cp WHERE cp.repository = :repository ORDER BY cp.frequency DESC")
    List<CodePattern> findMostFrequentPatterns(Repository repository);

    /**
     * Count patterns by repository
     */
    long countByRepository(Repository repository);
}
