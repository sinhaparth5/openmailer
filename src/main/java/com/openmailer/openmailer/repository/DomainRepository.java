package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Domain entity.
 * Provides CRUD operations and custom query methods for domain management.
 */
@Repository
public interface DomainRepository extends JpaRepository<Domain, String> {

  /**
   * Find all domains for a specific user.
   *
   * @param userId the user ID
   * @return list of domains
   */
  List<Domain> findByUserId(String userId);

  /**
   * Find all domains for a specific user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of domains
   */
  Page<Domain> findByUserId(String userId, Pageable pageable);

  /**
   * Find domain by ID and user ID.
   *
   * @param id the domain ID
   * @param userId the user ID
   * @return Optional containing the domain if found
   */
  Optional<Domain> findByIdAndUserId(String id, String userId);

  /**
   * Find domain by domain name.
   *
   * @param domainName the domain name
   * @return Optional containing the domain if found
   */
  Optional<Domain> findByDomainName(String domainName);

  /**
   * Find domain by domain name and user ID.
   *
   * @param domainName the domain name
   * @param userId the user ID
   * @return Optional containing the domain if found
   */
  Optional<Domain> findByDomainNameAndUserId(String domainName, String userId);

  /**
   * Find domains by verification status.
   *
   * @param userId the user ID
   * @param status the verification status
   * @param pageable pagination information
   * @return page of domains
   */
  Page<Domain> findByUserIdAndStatus(String userId, String status, Pageable pageable);

  /**
   * Find all verified domains for a user.
   *
   * @param userId the user ID
   * @return list of verified domains
   */
  List<Domain> findByUserIdAndStatus(String userId, String status);

  /**
   * Find domains that need verification (pending status).
   *
   * @param status the status to filter by (typically "PENDING")
   * @return list of domains needing verification
   */
  List<Domain> findByStatus(String status);

  /**
   * Find domains verified before a certain date (for re-verification).
   *
   * @param status the verification status
   * @param before the date threshold
   * @return list of domains to re-verify
   */
  List<Domain> findByStatusAndVerifiedAtBefore(String status, LocalDateTime before);

  /**
   * Check if domain name exists.
   *
   * @param domainName the domain name
   * @return true if exists, false otherwise
   */
  boolean existsByDomainName(String domainName);

  /**
   * Count domains for a specific user.
   *
   * @param userId the user ID
   * @return count of domains
   */
  long countByUserId(String userId);

  /**
   * Count verified domains for a user.
   *
   * @param userId the user ID
   * @param status the verification status
   * @return count of verified domains
   */
  long countByUserIdAndStatus(String userId, String status);

  /**
   * Delete all domains for a specific user.
   *
   * @param userId the user ID
   */
  void deleteByUserId(String userId);
}
