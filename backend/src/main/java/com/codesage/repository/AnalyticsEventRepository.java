package com.codesage.repository;

import com.codesage.model.AnalyticsEvent;
import com.codesage.model.Repository;
import com.codesage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    /**
     * Find events by type
     */
    List<AnalyticsEvent> findByEventType(String eventType);

    /**
     * Find events by user
     */
    List<AnalyticsEvent> findByUser(User user);

    /**
     * Find events by repository
     */
    List<AnalyticsEvent> findByRepository(Repository repository);

    /**
     * Find events in date range
     */
    @Query("SELECT ae FROM AnalyticsEvent ae WHERE ae.createdAt BETWEEN :startDate AND :endDate")
    List<AnalyticsEvent> findEventsBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find events by type in date range
     */
    @Query("SELECT ae FROM AnalyticsEvent ae WHERE ae.eventType = :eventType AND ae.createdAt BETWEEN :startDate AND :endDate")
    List<AnalyticsEvent> findEventsByTypeAndDateRange(String eventType, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count events by type
     */
    long countByEventType(String eventType);
}
