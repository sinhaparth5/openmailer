package com.openmailer.openmailer.service.security;

import com.openmailer.openmailer.exception.RateLimitExceededException;
import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.model.RateLimit;
import com.openmailer.openmailer.repository.RateLimitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for RateLimit management operations.
 * Handles rate limiting, quota tracking, and enforcement.
 */
@Service
@Transactional
public class RateLimitService {

  private final RateLimitRepository rateLimitRepository;

  @Autowired
  public RateLimitService(RateLimitRepository rateLimitRepository) {
    this.rateLimitRepository = rateLimitRepository;
  }

  /**
   * Create a new rate limit.
   *
   * @param rateLimit the rate limit to create
   * @return the created rate limit
   */
  public RateLimit createRateLimit(RateLimit rateLimit) {
    if (rateLimit.getRequestCount() == null) {
      rateLimit.setRequestCount(0);
    }
    return rateLimitRepository.save(rateLimit);
  }

  /**
   * Find rate limit by ID.
   *
   * @param id the rate limit ID
   * @return the rate limit
   * @throws ResourceNotFoundException if rate limit not found
   */
  @Transactional(readOnly = true)
  public RateLimit findById(Long id) {
    return rateLimitRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RateLimit", "id", id));
  }

  /**
   * Find all rate limits for a user.
   *
   * @param userId the user ID
   * @return list of rate limits
   */
  @Transactional(readOnly = true)
  public List<RateLimit> findByUser(Long userId) {
    return rateLimitRepository.findByUserId(userId);
  }

  /**
   * Find all rate limits for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of rate limits
   */
  @Transactional(readOnly = true)
  public Page<RateLimit> findByUser(Long userId, Pageable pageable) {
    return rateLimitRepository.findByUserId(userId, pageable);
  }

  /**
   * Find rate limits by resource type.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param pageable pagination information
   * @return page of rate limits
   */
  @Transactional(readOnly = true)
  public Page<RateLimit> findByResourceType(Long userId, String resourceType, Pageable pageable) {
    return rateLimitRepository.findByResourceType(userId, resourceType, pageable);
  }

  /**
   * Get or create active rate limit for user and resource.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param limitValue the limit value
   * @param windowDurationMinutes the window duration in minutes
   * @return the active rate limit
   */
  public RateLimit getOrCreateActiveRateLimit(Long userId, String resourceType, Integer limitValue, Integer windowDurationMinutes) {
    LocalDateTime now = LocalDateTime.now();

    return rateLimitRepository.findActiveRateLimit(userId, resourceType, now)
        .orElseGet(() -> {
          RateLimit newLimit = new RateLimit();
          newLimit.getUser().setId(userId);
          newLimit.setResourceType(resourceType);
          newLimit.setWindowStart(now);
          newLimit.setWindowEnd(now.plusMinutes(windowDurationMinutes));
          newLimit.setLimitValue(limitValue);
          newLimit.setRequestCount(0);
          return createRateLimit(newLimit);
        });
  }

  /**
   * Check if rate limit is exceeded.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param limitValue the limit value
   * @param windowDurationMinutes the window duration in minutes
   * @return true if limit exceeded, false otherwise
   */
  @Transactional(readOnly = true)
  public boolean isRateLimitExceeded(Long userId, String resourceType, Integer limitValue, Integer windowDurationMinutes) {
    LocalDateTime now = LocalDateTime.now();
    RateLimit rateLimit = rateLimitRepository.findActiveRateLimit(userId, resourceType, now).orElse(null);

    if (rateLimit == null) {
      return false;
    }

    return rateLimit.getRequestCount() >= rateLimit.getLimitValue();
  }

  /**
   * Increment request count for rate limit.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param limitValue the limit value
   * @param windowDurationMinutes the window duration in minutes
   * @return the updated rate limit
   * @throws RateLimitExceededException if rate limit is exceeded
   */
  public RateLimit incrementRequestCount(Long userId, String resourceType, Integer limitValue, Integer windowDurationMinutes) {
    RateLimit rateLimit = getOrCreateActiveRateLimit(userId, resourceType, limitValue, windowDurationMinutes);

    if (rateLimit.getRequestCount() >= rateLimit.getLimitValue()) {
      LocalDateTime now = LocalDateTime.now();
      long retryAfterSeconds = java.time.Duration.between(now, rateLimit.getWindowEnd()).getSeconds();

      throw new RateLimitExceededException(
          "Rate limit exceeded for " + resourceType,
          rateLimit.getLimitValue(),
          retryAfterSeconds
      );
    }

    rateLimit.setRequestCount(rateLimit.getRequestCount() + 1);
    return rateLimitRepository.save(rateLimit);
  }

  /**
   * Reset rate limit for user and resource.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   */
  public void resetRateLimit(Long userId, String resourceType) {
    LocalDateTime now = LocalDateTime.now();
    RateLimit rateLimit = rateLimitRepository.findActiveRateLimit(userId, resourceType, now).orElse(null);

    if (rateLimit != null) {
      rateLimit.setRequestCount(0);
      rateLimitRepository.save(rateLimit);
    }
  }

  /**
   * Get remaining requests for rate limit.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   * @param limitValue the limit value
   * @return remaining request count, or limit value if no active limit
   */
  @Transactional(readOnly = true)
  public int getRemainingRequests(Long userId, String resourceType, Integer limitValue) {
    LocalDateTime now = LocalDateTime.now();
    RateLimit rateLimit = rateLimitRepository.findActiveRateLimit(userId, resourceType, now).orElse(null);

    if (rateLimit == null) {
      return limitValue;
    }

    return Math.max(0, rateLimit.getLimitValue() - rateLimit.getRequestCount());
  }

  /**
   * Find exceeded rate limits for a user.
   *
   * @param userId the user ID
   * @return list of exceeded rate limits
   */
  @Transactional(readOnly = true)
  public List<RateLimit> findExceededLimits(Long userId) {
    return rateLimitRepository.findExceededLimits(userId);
  }

  /**
   * Clean up expired rate limits.
   *
   * @return count of deleted rate limits
   */
  public void cleanupExpiredRateLimits() {
    LocalDateTime now = LocalDateTime.now();
    rateLimitRepository.deleteExpiredRateLimits(now);
  }

  /**
   * Count rate limits for a user.
   *
   * @param userId the user ID
   * @return count of rate limits
   */
  @Transactional(readOnly = true)
  public long countByUser(Long userId) {
    return rateLimitRepository.countByUserId(userId);
  }

  /**
   * Count active rate limits for a user.
   *
   * @param userId the user ID
   * @return count of active rate limits
   */
  @Transactional(readOnly = true)
  public long countActiveRateLimits(Long userId) {
    LocalDateTime now = LocalDateTime.now();
    return rateLimitRepository.countActiveRateLimits(userId, now);
  }

  /**
   * Delete all rate limits for a user.
   *
   * @param userId the user ID
   */
  public void deleteAllByUser(Long userId) {
    rateLimitRepository.deleteByUserId(userId);
  }

  /**
   * Delete rate limits by resource type.
   *
   * @param userId the user ID
   * @param resourceType the resource type
   */
  public void deleteByResourceType(Long userId, String resourceType) {
    rateLimitRepository.deleteByResourceType(userId, resourceType);
  }
}
