package com.openmailer.openmailer.service.security;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.UnauthorizedException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.ApiKey;
import com.openmailer.openmailer.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Service class for ApiKey management operations.
 * Handles API key generation, validation, and lifecycle management.
 */
@Service
@Transactional
public class ApiKeyService {

  private final ApiKeyRepository apiKeyRepository;
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final int API_KEY_LENGTH = 48; // 48 bytes = 64 base64 characters

  @Autowired
  public ApiKeyService(ApiKeyRepository apiKeyRepository) {
    this.apiKeyRepository = apiKeyRepository;
  }

  /**
   * Create a new API key.
   *
   * @param apiKey the API key to create
   * @return the created API key
   */
  public ApiKey createApiKey(ApiKey apiKey) {
    // Generate API key if not provided
    if (apiKey.getApiKey() == null || apiKey.getApiKey().isEmpty()) {
      apiKey.setApiKey(generateApiKey());
    }

    // Generate key prefix if not provided
    if (apiKey.getKeyPrefix() == null || apiKey.getKeyPrefix().isEmpty()) {
      apiKey.setKeyPrefix(apiKey.getApiKey().substring(0, 8));
    }

    // Validate uniqueness
    if (apiKeyRepository.existsByApiKey(apiKey.getApiKey())) {
      throw new ValidationException("API key already exists", "apiKey");
    }
    if (apiKeyRepository.existsByKeyPrefix(apiKey.getKeyPrefix())) {
      throw new ValidationException("Key prefix already exists", "keyPrefix");
    }

    // Set defaults
    if (apiKey.getIsActive() == null) {
      apiKey.setIsActive(true);
    }

    return apiKeyRepository.save(apiKey);
  }

  /**
   * Find API key by ID.
   *
   * @param id the API key ID
   * @return the API key
   * @throws ResourceNotFoundException if API key not found
   */
  @Transactional(readOnly = true)
  public ApiKey findById(Long id) {
    return apiKeyRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", id));
  }

  /**
   * Find API key by ID and user ID.
   *
   * @param id the API key ID
   * @param userId the user ID
   * @return the API key
   * @throws ResourceNotFoundException if API key not found
   */
  @Transactional(readOnly = true)
  public ApiKey findByIdAndUserId(Long id, Long userId) {
    return apiKeyRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", id));
  }

  /**
   * Find all API keys for a user.
   *
   * @param userId the user ID
   * @return list of API keys
   */
  @Transactional(readOnly = true)
  public List<ApiKey> findByUserId(Long userId) {
    return apiKeyRepository.findByUserId(userId);
  }

  /**
   * Find all API keys for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of API keys
   */
  @Transactional(readOnly = true)
  public Page<ApiKey> findByUserId(Long userId, Pageable pageable) {
    return apiKeyRepository.findByUserId(userId, pageable);
  }

  /**
   * Find active API keys for a user.
   *
   * @param userId the user ID
   * @param isActive true for active, false for inactive
   * @param pageable pagination information
   * @return page of API keys
   */
  @Transactional(readOnly = true)
  public Page<ApiKey> findByStatus(Long userId, Boolean isActive, Pageable pageable) {
    return apiKeyRepository.findByUserIdAndIsActive(userId, isActive, pageable);
  }

  /**
   * Validate API key and return if valid.
   *
   * @param apiKey the API key value
   * @return the validated API key
   * @throws UnauthorizedException if key is invalid, inactive, or expired
   */
  @Transactional(readOnly = true)
  public ApiKey validateApiKey(String apiKey) {
    ApiKey key = apiKeyRepository.findByApiKey(apiKey)
        .orElseThrow(() -> new UnauthorizedException("Invalid API key"));

    if (!key.getIsActive()) {
      throw new UnauthorizedException("API key is inactive");
    }

    if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new UnauthorizedException("API key has expired");
    }

    return key;
  }

  /**
   * Update last used timestamp for an API key.
   *
   * @param apiKey the API key value
   */
  public void updateLastUsed(String apiKey) {
    ApiKey key = apiKeyRepository.findByApiKey(apiKey).orElse(null);
    if (key != null) {
      key.setLastUsedAt(LocalDateTime.now());
      apiKeyRepository.save(key);
    }
  }

  /**
   * Revoke an API key.
   *
   * @param id the API key ID
   * @param userId the user ID
   * @return the revoked API key
   */
  public ApiKey revokeApiKey(Long id, Long userId) {
    ApiKey apiKey = findByIdAndUserId(id, userId);
    apiKey.setIsActive(false);
    return apiKeyRepository.save(apiKey);
  }

  /**
   * Activate an API key.
   *
   * @param id the API key ID
   * @param userId the user ID
   * @return the activated API key
   */
  public ApiKey activateApiKey(Long id, Long userId) {
    ApiKey apiKey = findByIdAndUserId(id, userId);
    apiKey.setIsActive(true);
    return apiKeyRepository.save(apiKey);
  }

  /**
   * Update API key name.
   *
   * @param id the API key ID
   * @param userId the user ID
   * @param newName the new name
   * @return the updated API key
   */
  public ApiKey updateKeyName(Long id, Long userId, String newName) {
    ApiKey apiKey = findByIdAndUserId(id, userId);
    apiKey.setKeyName(newName);
    return apiKeyRepository.save(apiKey);
  }

  /**
   * Update API key scopes.
   *
   * @param id the API key ID
   * @param userId the user ID
   * @param scopes the new scopes
   * @return the updated API key
   */
  public ApiKey updateScopes(Long id, Long userId, String[] scopes) {
    ApiKey apiKey = findByIdAndUserId(id, userId);
    apiKey.setScopes(scopes);
    return apiKeyRepository.save(apiKey);
  }

  /**
   * Update API key expiration.
   *
   * @param id the API key ID
   * @param userId the user ID
   * @param expiresAt the new expiration date
   * @return the updated API key
   */
  public ApiKey updateExpiration(Long id, Long userId, LocalDateTime expiresAt) {
    ApiKey apiKey = findByIdAndUserId(id, userId);
    apiKey.setExpiresAt(expiresAt);
    return apiKeyRepository.save(apiKey);
  }

  /**
   * Deactivate expired API keys.
   *
   * @return count of deactivated keys
   */
  public int deactivateExpiredKeys() {
    List<ApiKey> expiredKeys = apiKeyRepository.findExpiredKeys(LocalDateTime.now());
    for (ApiKey key : expiredKeys) {
      key.setIsActive(false);
      apiKeyRepository.save(key);
    }
    return expiredKeys.size();
  }

  /**
   * Find API keys expiring soon.
   *
   * @param days number of days threshold
   * @return list of API keys expiring soon
   */
  @Transactional(readOnly = true)
  public List<ApiKey> findKeysExpiringSoon(int days) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime threshold = now.plusDays(days);
    return apiKeyRepository.findKeysExpiringSoon(now, threshold);
  }

  /**
   * Delete an API key.
   *
   * @param id the API key ID
   * @param userId the user ID
   */
  public void deleteApiKey(Long id, Long userId) {
    ApiKey apiKey = findByIdAndUserId(id, userId);
    apiKeyRepository.delete(apiKey);
  }

  /**
   * Delete all API keys for a user.
   *
   * @param userId the user ID
   */
  public void deleteAllByUserId(Long userId) {
    apiKeyRepository.deleteByUserId(userId);
  }

  /**
   * Count API keys for a user.
   *
   * @param userId the user ID
   * @return count of API keys
   */
  @Transactional(readOnly = true)
  public long countByUserId(Long userId) {
    return apiKeyRepository.countByUserId(userId);
  }

  /**
   * Count active API keys for a user.
   *
   * @param userId the user ID
   * @return count of active API keys
   */
  @Transactional(readOnly = true)
  public long countActiveByUserId(Long userId) {
    return apiKeyRepository.countActiveByUserId(userId);
  }

  /**
   * Generate a secure random API key.
   *
   * @return generated API key
   */
  private String generateApiKey() {
    byte[] randomBytes = new byte[API_KEY_LENGTH];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }
}
