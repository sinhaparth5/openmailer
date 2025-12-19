package com.openmailer.openmailer.service.domain;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Domain;
import com.openmailer.openmailer.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for Domain management operations.
 * Handles domain CRUD operations, DNS verification, and user authorization.
 */
@Service
@Transactional
public class DomainService {

  private final DomainRepository domainRepository;

  @Autowired
  public DomainService(DomainRepository domainRepository) {
    this.domainRepository = domainRepository;
  }

  /**
   * Create a new domain.
   *
   * @param domain the domain to create
   * @return the created domain
   * @throws ValidationException if domain name already exists
   */
  public Domain createDomain(Domain domain) {
    // Validate domain name uniqueness
    if (domainRepository.existsByDomainName(domain.getDomainName())) {
      throw new ValidationException("Domain name already exists", "domainName");
    }

    // Set default status
    if (domain.getStatus() == null) {
      domain.setStatus("PENDING");
    }

    domain.setCreatedAt(LocalDateTime.now());
    domain.setUpdatedAt(LocalDateTime.now());

    return domainRepository.save(domain);
  }

  /**
   * Find domain by ID.
   *
   * @param id the ID (String)
   * @return the domain
   * @throws ResourceNotFoundException if domain not found
   */
  @Transactional(readOnly = true)
  public Domain findById(String id) {
    return domainRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Domain", "id", id));
  }

  /**
   * Find domain by ID and verify user ownership.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @return the domain
   * @throws ResourceNotFoundException if domain not found
   */
  @Transactional(readOnly = true)
  public Domain findByIdAndUserId(String id, String userId) {
    return domainRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Domain", "id", id));
  }

  /**
   * Find domain by name.
   *
   * @param domainName the domain name
   * @return the domain
   * @throws ResourceNotFoundException if domain not found
   */
  @Transactional(readOnly = true)
  public Domain findByDomainName(String domainName) {
    return domainRepository.findByDomainName(domainName)
        .orElseThrow(() -> new ResourceNotFoundException("Domain", "domainName", domainName));
  }

  /**
   * Find all domains for a user.
   *
   * @param userId the ID (String)
   * @return list of domains
   */
  @Transactional(readOnly = true)
  public List<Domain> findByUserId(String userId) {
    return domainRepository.findByUserId(userId);
  }

  /**
   * Find all domains for a user with pagination.
   *
   * @param userId the ID (String)
   * @param pageable pagination information
   * @return page of domains
   */
  @Transactional(readOnly = true)
  public Page<Domain> findByUserId(String userId, Pageable pageable) {
    return domainRepository.findByUserId(userId, pageable);
  }

  /**
   * Find domains by status.
   *
   * @param userId the ID (String)
   * @param status the verification status
   * @param pageable pagination information
   * @return page of domains
   */
  @Transactional(readOnly = true)
  public Page<Domain> findByStatus(String userId, String status, Pageable pageable) {
    return domainRepository.findByUserIdAndStatus(userId, status, pageable);
  }

  /**
   * Find all verified domains for a user.
   *
   * @param userId the ID (String)
   * @return list of verified domains
   */
  @Transactional(readOnly = true)
  public List<Domain> findVerifiedDomains(String userId) {
    return domainRepository.findByUserIdAndStatus(userId, "VERIFIED");
  }

  /**
   * Find domains pending verification.
   *
   * @return list of pending domains
   */
  @Transactional(readOnly = true)
  public List<Domain> findPendingDomains() {
    return domainRepository.findByStatus("PENDING");
  }

  /**
   * Find domains that need re-verification.
   *
   * @param daysThreshold number of days since last verification
   * @return list of domains to re-verify
   */
  @Transactional(readOnly = true)
  public List<Domain> findDomainsForReverification(int daysThreshold) {
    LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);
    return domainRepository.findByStatusAndVerifiedAtBefore("VERIFIED", threshold);
  }

  /**
   * Update domain verification status.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param status the new status
   * @param spfVerified SPF verification result
   * @param dkimVerified DKIM verification result
   * @param dmarcVerified DMARC verification result
   * @return the updated domain
   */
  public Domain updateVerificationStatus(String id, String userId, String status,
                                          Boolean spfVerified, Boolean dkimVerified, Boolean dmarcVerified) {
    Domain domain = findByIdAndUserId(id, userId);

    domain.setStatus(status);
    domain.setSpfVerified(spfVerified);
    domain.setDkimVerified(dkimVerified);
    domain.setDmarcVerified(dmarcVerified);

    if ("VERIFIED".equals(status)) {
      domain.setVerifiedAt(LocalDateTime.now());
    }

    domain.setLastCheckedAt(LocalDateTime.now());
    domain.setUpdatedAt(LocalDateTime.now());

    return domainRepository.save(domain);
  }

  /**
   * Update domain DNS records.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param spfRecord SPF record value
   * @param dkimRecord DKIM record value
   * @param dmarcRecord DMARC record value
   * @return the updated domain
   */
  public Domain updateDnsRecords(String id, String userId, String spfRecord, String dkimRecord, String dmarcRecord) {
    Domain domain = findByIdAndUserId(id, userId);

    domain.setSpfRecord(spfRecord);
    domain.setDkimRecord(dkimRecord);
    domain.setDmarcRecord(dmarcRecord);
    domain.setUpdatedAt(LocalDateTime.now());

    return domainRepository.save(domain);
  }

  /**
   * Update DKIM key pair.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param dkimPublicKey the public key
   * @param dkimPrivateKey the encrypted private key
   * @return the updated domain
   */
  public Domain updateDkimKeys(String id, String userId, String dkimPublicKey, String dkimPrivateKey) {
    Domain domain = findByIdAndUserId(id, userId);

    domain.setDkimPublicKey(dkimPublicKey);
    domain.setDkimPrivateKey(dkimPrivateKey);
    domain.setUpdatedAt(LocalDateTime.now());

    return domainRepository.save(domain);
  }

  /**
   * Delete a domain.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @throws ResourceNotFoundException if domain not found
   */
  public void deleteDomain(String id, String userId) {
    Domain domain = findByIdAndUserId(id, userId);
    domainRepository.delete(domain);
  }

  /**
   * Delete all domains for a user (GDPR compliance).
   *
   * @param userId the ID (String)
   */
  public void deleteAllByUserId(String userId) {
    domainRepository.deleteByUserId(userId);
  }

  /**
   * Count domains for a user.
   *
   * @param userId the ID (String)
   * @return count of domains
   */
  @Transactional(readOnly = true)
  public long countByUserId(String userId) {
    return domainRepository.countByUserId(userId);
  }

  /**
   * Count verified domains for a user.
   *
   * @param userId the ID (String)
   * @return count of verified domains
   */
  @Transactional(readOnly = true)
  public long countVerifiedDomains(String userId) {
    return domainRepository.countByUserIdAndStatus(userId, "VERIFIED");
  }

  /**
   * Check if domain name exists.
   *
   * @param domainName the domain name
   * @return true if exists, false otherwise
   */
  @Transactional(readOnly = true)
  public boolean domainNameExists(String domainName) {
    return domainRepository.existsByDomainName(domainName);
  }
}
