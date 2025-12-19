package com.openmailer.openmailer.service.provider;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.repository.EmailProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for EmailProvider management operations.
 * Handles provider CRUD operations, credentials encryption, and user authorization.
 */
@Service
@Transactional
public class EmailProviderService {

  private final EmailProviderRepository providerRepository;

  @Autowired
  public EmailProviderService(EmailProviderRepository providerRepository) {
    this.providerRepository = providerRepository;
  }

  /**
   * Create a new email provider.
   *
   * @param provider the provider to create
   * @return the created provider
   * @throws ValidationException if provider name already exists for user
   */
  public EmailProvider createProvider(EmailProvider provider) {
    // Validate provider name uniqueness for user
    if (providerRepository.findByNameAndUserId(provider.getName(), provider.getUserId()).isPresent()) {
      throw new ValidationException("Provider name already exists", "name");
    }

    // If this is set as default, unset other defaults
    if (provider.getIsDefault() != null && provider.getIsDefault()) {
      unsetDefaultProvider(provider.getUserId());
    }

    provider.setCreatedAt(LocalDateTime.now());
    provider.setUpdatedAt(LocalDateTime.now());

    return providerRepository.save(provider);
  }

  /**
   * Find provider by ID.
   *
   * @param id the ID (String)
   * @return the provider
   * @throws ResourceNotFoundException if provider not found
   */
  @Transactional(readOnly = true)
  public EmailProvider findById(String id) {
    return providerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmailProvider", "id", id));
  }

  /**
   * Find provider by ID and verify user ownership.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @return the provider
   * @throws ResourceNotFoundException if provider not found
   */
  @Transactional(readOnly = true)
  public EmailProvider findByIdAndUserId(String id, String userId) {
    return providerRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("EmailProvider", "id", id));
  }

  /**
   * Find all providers for a user.
   *
   * @param userId the ID (String)
   * @return list of providers
   */
  @Transactional(readOnly = true)
  public List<EmailProvider> findByUserId(String userId) {
    return providerRepository.findByUserId(userId);
  }

  /**
   * Find all providers for a user with pagination.
   *
   * @param userId the ID (String)
   * @param pageable pagination information
   * @return page of providers
   */
  @Transactional(readOnly = true)
  public Page<EmailProvider> findByUserId(String userId, Pageable pageable) {
    return providerRepository.findByUserId(userId, pageable);
  }

  /**
   * Find active providers for a user.
   *
   * @param userId the ID (String)
   * @return list of active providers
   */
  @Transactional(readOnly = true)
  public List<EmailProvider> findActiveProviders(String userId) {
    return providerRepository.findByUserIdAndIsActive(userId, true);
  }

  /**
   * Find providers by type.
   *
   * @param userId the ID (String)
   * @param providerType the provider type (AWS_SES, SENDGRID, SMTP)
   * @param pageable pagination information
   * @return page of providers
   */
  @Transactional(readOnly = true)
  public Page<EmailProvider> findByType(String userId, String providerType, Pageable pageable) {
    return providerRepository.findByUserIdAndProviderType(userId, providerType, pageable);
  }

  /**
   * Find default provider for a user.
   *
   * @param userId the ID (String)
   * @return the default provider
   * @throws ResourceNotFoundException if no default provider found
   */
  @Transactional(readOnly = true)
  public EmailProvider findDefaultProvider(String userId) {
    return providerRepository.findByUserIdAndIsDefault(userId, true)
        .orElseThrow(() -> new ResourceNotFoundException("No default email provider configured"));
  }

  /**
   * Update an existing provider.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param updatedProvider the updated provider data
   * @return the updated provider
   * @throws ResourceNotFoundException if provider not found
   */
  public EmailProvider updateProvider(String id, String userId, EmailProvider updatedProvider) {
    EmailProvider provider = findByIdAndUserId(id, userId);

    if (updatedProvider.getName() != null && !updatedProvider.getName().equals(provider.getName())) {
      // Check name uniqueness
      if (providerRepository.findByNameAndUserId(updatedProvider.getName(), userId).isPresent()) {
        throw new ValidationException("Provider name already exists", "name");
      }
      provider.setName(updatedProvider.getName());
    }

    if (updatedProvider.getProviderType() != null) {
      provider.setProviderType(updatedProvider.getProviderType());
    }
    if (updatedProvider.getConfiguration() != null) {
      provider.setConfiguration(updatedProvider.getConfiguration());
    }
    if (updatedProvider.getIsActive() != null) {
      provider.setIsActive(updatedProvider.getIsActive());
    }
    if (updatedProvider.getIsDefault() != null && updatedProvider.getIsDefault()) {
      unsetDefaultProvider(userId);
      provider.setIsDefault(true);
    }

    provider.setUpdatedAt(LocalDateTime.now());
    return providerRepository.save(provider);
  }

  /**
   * Set provider as default.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @return the updated provider
   */
  public EmailProvider setAsDefault(String id, String userId) {
    EmailProvider provider = findByIdAndUserId(id, userId);

    // Unset other defaults
    unsetDefaultProvider(userId);

    provider.setIsDefault(true);
    provider.setUpdatedAt(LocalDateTime.now());

    return providerRepository.save(provider);
  }

  /**
   * Toggle provider active status.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param isActive the active status
   * @return the updated provider
   */
  public EmailProvider setActiveStatus(String id, String userId, Boolean isActive) {
    EmailProvider provider = findByIdAndUserId(id, userId);

    provider.setIsActive(isActive);
    provider.setUpdatedAt(LocalDateTime.now());

    return providerRepository.save(provider);
  }

  /**
   * Update provider sending statistics.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param emailsSent increment sent count
   * @param emailsFailed increment failed count
   * @return the updated provider
   */
  public EmailProvider updateStats(String id, String userId, int emailsSent, int emailsFailed) {
    EmailProvider provider = findByIdAndUserId(id, userId);

    if (emailsSent > 0) {
      provider.setEmailsSent(provider.getEmailsSent() + emailsSent);
    }
    if (emailsFailed > 0) {
      provider.setEmailsFailed(provider.getEmailsFailed() + emailsFailed);
    }

    provider.setLastUsedAt(LocalDateTime.now());
    provider.setUpdatedAt(LocalDateTime.now());

    return providerRepository.save(provider);
  }

  /**
   * Delete a provider.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @throws ResourceNotFoundException if provider not found
   * @throws ValidationException if trying to delete the only provider
   */
  public void deleteProvider(String id, String userId) {
    EmailProvider provider = findByIdAndUserId(id, userId);

    // Check if this is the only provider
    long providerCount = providerRepository.countByUserId(userId);
    if (providerCount <= 1) {
      throw new ValidationException("Cannot delete the only email provider", "provider");
    }

    providerRepository.delete(provider);
  }

  /**
   * Delete all providers for a user (GDPR compliance).
   *
   * @param userId the ID (String)
   */
  public void deleteAllByUserId(String userId) {
    providerRepository.deleteByUserId(userId);
  }

  /**
   * Count providers for a user.
   *
   * @param userId the ID (String)
   * @return count of providers
   */
  @Transactional(readOnly = true)
  public long countByUserId(String userId) {
    return providerRepository.countByUserId(userId);
  }

  /**
   * Count active providers for a user.
   *
   * @param userId the ID (String)
   * @return count of active providers
   */
  @Transactional(readOnly = true)
  public long countActiveProviders(String userId) {
    return providerRepository.countByUserIdAndIsActive(userId, true);
  }

  /**
   * Unset default flag from all providers for a user.
   *
   * @param userId the ID (String)
   */
  private void unsetDefaultProvider(String userId) {
    List<EmailProvider> providers = providerRepository.findByUserId(userId);
    providers.forEach(p -> {
      if (p.getIsDefault() != null && p.getIsDefault()) {
        p.setIsDefault(false);
        providerRepository.save(p);
      }
    });
  }
}
