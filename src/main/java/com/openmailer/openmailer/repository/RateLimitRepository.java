package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.RateLimit;
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
 * Repository interface for RateLimit entity.
 * Provides CRUD operations and custom query methods for rate limiting.
 */
@Repository
public interface RateLimitRepository extends JpaRepository<RateLimit, String> {

  /**
   * Find all rate limits for a user.
   *
   * @param userId the user ID
   * @return list of rate limits
   */
  @Query("SELECT r FROM RateLimit r WHERE r.user.id = :userId")
  List<RateLimit> findByUserId(@Param("userId") String userId);

  /**
   * Find all rate limits for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of rate limits
   */
  @Query("SELECT r FROM RateLimit r WHERE r.user.id = :userId")
  Page<RateLimit> findByUserId(@Param("userId") String userId, Pageable pageable);

  /**
   * Find active rate limit for user and resource type.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param currentTime current timestamp
   * @return Optional containing the active rate limit if found
   */
  @Query("SELECT r FROM RateLimit r WHERE r.user.id = :userId AND r.resourceType = :resourceType AND r.windowStart <= :currentTime AND r.windowEnd > :currentTime")
  Optional<RateLimit> findActiveRateLimit(@Param("userId") String userId, @Param("resourceType") String resourceType, @Param("currentTime") LocalDateTime currentTime);

  /**
   * Find rate limits by resource type.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param pageable pagination information
   * @return page of rate limits
   */
  @Query("SELECT r FROM RateLimit r WHERE r.user.id = :userId AND r.resourceType = :resourceType")
  Page<RateLimit> findByResourceType(@Param("userId") String userId, @Param("resourceType") String resourceType, Pageable pageable);

  /**
   * Find rate limits within a time window.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param startTime start time
   * @param endTime end time
   * @return list of rate limits
   */
  @Query("SELECT r FROM RateLimit r WHERE r.user.id = :userId AND r.resourceType = :resourceType AND r.windowStart >= :startTime AND r.windowEnd <= :endTime")
  List<RateLimit> findByTimeWindow(@Param("userId") String userId, @Param("resourceType") String resourceType, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

  /**
   * Find expired rate limits.
   *
   * @param currentTime current timestamp
   * @return list of expired rate limits
   */
  @Query("SELECT r FROM RateLimit r WHERE r.windowEnd < :currentTime")
  List<RateLimit> findExpiredRateLimits(@Param("currentTime") LocalDateTime currentTime);

  /**
   * Find rate limits exceeding the limit.
   *
   * @param userId the user ID
   * @return list of rate limits where request count exceeds limit value
   */
  @Query("SELECT r FROM RateLimit r WHERE r.user.id = :userId AND r.requestCount >= r.limitValue")
  List<RateLimit> findExceededLimits(@Param("userId") String userId);

  /**
   * Count rate limits for a user.
   *
   * @param userId the user ID
   * @return count of rate limits
   */
  @Query("SELECT COUNT(r) FROM RateLimit r WHERE r.user.id = :userId")
  long countByUserId(@Param("userId") String userId);

  /**
   * Count active rate limits for a user.
   *
   * @param userId the user ID
   * @param currentTime current timestamp
   * @return count of active rate limits
   */
  @Query("SELECT COUNT(r) FROM RateLimit r WHERE r.user.id = :userId AND r.windowStart <= :currentTime AND r.windowEnd > :currentTime")
  long countActiveRateLimits(@Param("userId") String userId, @Param("currentTime") LocalDateTime currentTime);

  /**
   * Delete expired rate limits.
   *
   * @param currentTime current timestamp
   */
  @Query("DELETE FROM RateLimit r WHERE r.windowEnd < :currentTime")
  void deleteExpiredRateLimits(@Param("currentTime") LocalDateTime currentTime);

  /**
   * Delete all rate limits for a user.
   *
   * @param userId the user ID
   */
  @Query("DELETE FROM RateLimit r WHERE r.user.id = :userId")
  void deleteByUserId(@Param("userId") String userId);

  /**
   * Delete rate limits by resource type.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   */
  @Query("DELETE FROM RateLimit r WHERE r.user.id = :userId AND r.resourceType = :resourceType")
  void deleteByResourceType(@Param("userId") String userId, @Param("resourceType") String resourceType);
}
