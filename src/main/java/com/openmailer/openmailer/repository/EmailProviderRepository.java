package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.EmailProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EmailProvider entity.
 * Provides CRUD operations and custom query methods for email provider management.
 */
@Repository
public interface EmailProviderRepository extends JpaRepository<EmailProvider, Long> {

  /**
   * Find all providers for a specific user.
   *
   * @param userId the user ID
   * @return list of email providers
   */
  List<EmailProvider> findByUserId(Long userId);

  /**
   * Find all providers for a specific user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of email providers
   */
  Page<EmailProvider> findByUserId(Long userId, Pageable pageable);

  /**
   * Find provider by ID and user ID.
   *
   * @param id the provider ID
   * @param userId the user ID
   * @return Optional containing the provider if found
   */
  Optional<EmailProvider> findByIdAndUserId(Long id, Long userId);

  /**
   * Find provider by name and user ID.
   *
   * @param name the provider name
   * @param userId the user ID
   * @return Optional containing the provider if found
   */
  Optional<EmailProvider> findByNameAndUserId(String name, Long userId);

  /**
   * Find providers by type.
   *
   * @param userId the user ID
   * @param providerType the provider type (AWS_SES, SENDGRID, SMTP)
   * @param pageable pagination information
   * @return page of providers
   */
  Page<EmailProvider> findByUserIdAndProviderType(Long userId, String providerType, Pageable pageable);

  /**
   * Find all active providers for a user.
   *
   * @param userId the user ID
   * @param isActive active status
   * @return list of active providers
   */
  List<EmailProvider> findByUserIdAndIsActive(Long userId, Boolean isActive);

  /**
   * Find providers by type and active status.
   *
   * @param userId the user ID
   * @param providerType the provider type
   * @param isActive active status
   * @return list of providers
   */
  List<EmailProvider> findByUserIdAndProviderTypeAndIsActive(Long userId, String providerType, Boolean isActive);

  /**
   * Find default provider for a user.
   *
   * @param userId the user ID
   * @param isDefault default flag
   * @return Optional containing the default provider if found
   */
  Optional<EmailProvider> findByUserIdAndIsDefault(Long userId, Boolean isDefault);

  /**
   * Count providers for a specific user.
   *
   * @param userId the user ID
   * @return count of providers
   */
  long countByUserId(Long userId);

  /**
   * Count active providers for a user.
   *
   * @param userId the user ID
   * @param isActive active status
   * @return count of active providers
   */
  long countByUserIdAndIsActive(Long userId, Boolean isActive);

  /**
   * Delete all providers for a specific user.
   *
   * @param userId the user ID
   */
  void deleteByUserId(Long userId);
}
