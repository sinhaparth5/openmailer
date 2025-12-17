package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for WebhookEvent entity.
 * Provides CRUD operations and custom query methods for webhook event management.
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

  /**
   * Find all webhook events for a user.
   *
   * @param userId the user ID
   * @return list of webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.user.id = :userId")
  List<WebhookEvent> findByUserId(@Param("userId") Long userId);

  /**
   * Find all webhook events for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.user.id = :userId")
  Page<WebhookEvent> findByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * Find webhook event by provider event ID.
   *
   * @param providerEventId the provider event ID
   * @return Optional containing the webhook event if found
   */
  Optional<WebhookEvent> findByProviderEventId(String providerEventId);

  /**
   * Find webhook events by event type.
   *
   * @param userId the user ID
   * @param eventType the event type
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.user.id = :userId AND w.eventType = :eventType")
  Page<WebhookEvent> findByEventType(@Param("userId") Long userId, @Param("eventType") String eventType, Pageable pageable);

  /**
   * Find webhook events by provider.
   *
   * @param providerId the provider ID
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.provider.id = :providerId")
  Page<WebhookEvent> findByProviderId(@Param("providerId") Long providerId, Pageable pageable);

  /**
   * Find unprocessed webhook events.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of unprocessed webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.user.id = :userId AND w.processed = false")
  Page<WebhookEvent> findUnprocessed(@Param("userId") Long userId, Pageable pageable);

  /**
   * Find unprocessed webhook events (all users).
   *
   * @return list of unprocessed webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.processed = false ORDER BY w.createdAt ASC")
  List<WebhookEvent> findAllUnprocessed();

  /**
   * Find processed webhook events.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of processed webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.user.id = :userId AND w.processed = true")
  Page<WebhookEvent> findProcessed(@Param("userId") Long userId, Pageable pageable);

  /**
   * Find failed webhook events (processed with errors).
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of failed webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.user.id = :userId AND w.processed = true AND w.errorMessage IS NOT NULL")
  Page<WebhookEvent> findFailed(@Param("userId") Long userId, Pageable pageable);

  /**
   * Find webhook events within a date range.
   *
   * @param userId the user ID
   * @param startDate start date
   * @param endDate end date
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Query("SELECT w FROM WebhookEvent w WHERE w.user.id = :userId AND w.createdAt BETWEEN :startDate AND :endDate")
  Page<WebhookEvent> findByDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

  /**
   * Count webhook events for a user.
   *
   * @param userId the user ID
   * @return count of webhook events
   */
  @Query("SELECT COUNT(w) FROM WebhookEvent w WHERE w.user.id = :userId")
  long countByUserId(@Param("userId") Long userId);

  /**
   * Count unprocessed webhook events for a user.
   *
   * @param userId the user ID
   * @return count of unprocessed webhook events
   */
  @Query("SELECT COUNT(w) FROM WebhookEvent w WHERE w.user.id = :userId AND w.processed = false")
  long countUnprocessed(@Param("userId") Long userId);

  /**
   * Count webhook events by event type.
   *
   * @param userId the user ID
   * @param eventType the event type
   * @return count of webhook events
   */
  @Query("SELECT COUNT(w) FROM WebhookEvent w WHERE w.user.id = :userId AND w.eventType = :eventType")
  long countByEventType(@Param("userId") Long userId, @Param("eventType") String eventType);

  /**
   * Check if provider event ID exists.
   *
   * @param providerEventId the provider event ID
   * @return true if exists, false otherwise
   */
  boolean existsByProviderEventId(String providerEventId);

  /**
   * Delete old processed webhook events.
   *
   * @param cutoffDate cutoff date
   */
  @Query("DELETE FROM WebhookEvent w WHERE w.processed = true AND w.createdAt < :cutoffDate")
  void deleteOldProcessedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

  /**
   * Delete all webhook events for a user.
   *
   * @param userId the user ID
   */
  @Query("DELETE FROM WebhookEvent w WHERE w.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
