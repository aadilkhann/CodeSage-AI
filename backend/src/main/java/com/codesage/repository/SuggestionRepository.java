package com.codesage.repository;

import com.codesage.model.Analysis;
import com.codesage.model.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, UUID> {

    /**
     * Find all suggestions for an analysis
     */
    List<Suggestion> findByAnalysis(Analysis analysis);

    /**
     * Find suggestions by status
     */
    List<Suggestion> findByAnalysisAndStatus(Analysis analysis, String status);

    /**
     * Find suggestions by severity
     */
    List<Suggestion> findByAnalysisAndSeverity(Analysis analysis, String severity);

    /**
     * Find suggestions by category
     */
    List<Suggestion> findByAnalysisAndCategory(Analysis analysis, String category);

    /**
     * Find high-confidence suggestions
     */
    @Query("SELECT s FROM Suggestion s WHERE s.analysis = :analysis AND s.confidenceScore >= :minConfidence")
    List<Suggestion> findHighConfidenceSuggestions(Analysis analysis, BigDecimal minConfidence);

    /**
     * Count suggestions by status
     */
    long countByAnalysisAndStatus(Analysis analysis, String status);

    /**
     * Calculate suggestion acceptance rate
     */
    @Query("SELECT (COUNT(s) * 100.0 / (SELECT COUNT(s2) FROM Suggestion s2)) FROM Suggestion s WHERE s.status = 'accepted'")
    Double calculateAcceptanceRate();
}
