package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.ApiKey;
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
 * Repository interface for ApiKey entity.
 * Provides CRUD operations and custom query methods for API key management.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  /**
   * Find all API keys for a user.
   *
   * @param userId the user ID
   * @return list of API keys
   */
  @Query("SELECT k FROM ApiKey k WHERE k.user.id = :userId")
  List<ApiKey> findByUserId(@Param("userId") Long userId);

  /**
   * Find all API keys for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of API keys
   */
  @Query("SELECT k FROM ApiKey k WHERE k.user.id = :userId")
  Page<ApiKey> findByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * Find API key by ID and user ID.
   *
   * @param id the API key ID
   * @param userId the user ID
   * @return Optional containing the API key if found
   */
  @Query("SELECT k FROM ApiKey k WHERE k.id = :id AND k.user.id = :userId")
  Optional<ApiKey> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  /**
   * Find API key by key value.
   *
   * @param apiKey the API key value
   * @return Optional containing the API key if found
   */
  Optional<ApiKey> findByApiKey(String apiKey);

  /**
   * Find API key by key prefix.
   *
   * @param keyPrefix the key prefix
   * @return Optional containing the API key if found
   */
  Optional<ApiKey> findByKeyPrefix(String keyPrefix);

  /**
   * Find active API keys for a user.
   *
   * @param userId the user ID
   * @param isActive true for active keys, false for inactive
   * @param pageable pagination information
   * @return page of API keys
   */
  @Query("SELECT k FROM ApiKey k WHERE k.user.id = :userId AND k.isActive = :isActive")
  Page<ApiKey> findByUserIdAndIsActive(@Param("userId") Long userId, @Param("isActive") Boolean isActive, Pageable pageable);

  /**
   * Find expired API keys.
   *
   * @param currentTime current timestamp
   * @return list of expired API keys
   */
  @Query("SELECT k FROM ApiKey k WHERE k.expiresAt IS NOT NULL AND k.expiresAt < :currentTime AND k.isActive = true")
  List<ApiKey> findExpiredKeys(@Param("currentTime") LocalDateTime currentTime);

  /**
   * Find API keys expiring soon.
   *
   * @param expiryThreshold threshold date
   * @return list of API keys expiring soon
   */
  @Query("SELECT k FROM ApiKey k WHERE k.expiresAt IS NOT NULL AND k.expiresAt BETWEEN :now AND :threshold AND k.isActive = true")
  List<ApiKey> findKeysExpiringSoon(@Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

  /**
   * Count API keys for a user.
   *
   * @param userId the user ID
   * @return count of API keys
   */
  @Query("SELECT COUNT(k) FROM ApiKey k WHERE k.user.id = :userId")
  long countByUserId(@Param("userId") Long userId);

  /**
   * Count active API keys for a user.
   *
   * @param userId the user ID
   * @return count of active API keys
   */
  @Query("SELECT COUNT(k) FROM ApiKey k WHERE k.user.id = :userId AND k.isActive = true")
  long countActiveByUserId(@Param("userId") Long userId);

  /**
   * Check if API key exists.
   *
   * @param apiKey the API key value
   * @return true if exists, false otherwise
   */
  boolean existsByApiKey(String apiKey);

  /**
   * Check if key prefix exists.
   *
   * @param keyPrefix the key prefix
   * @return true if exists, false otherwise
   */
  boolean existsByKeyPrefix(String keyPrefix);

  /**
   * Delete all API keys for a user.
   *
   * @param userId the user ID
   */
  @Query("DELETE FROM ApiKey k WHERE k.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
