package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.repository.EmailCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for EmailCampaign management operations.
 * Handles campaign CRUD operations with user authorization.
 */
@Service
@Transactional
public class CampaignService {

  private final EmailCampaignRepository campaignRepository;

  @Autowired
  public CampaignService(EmailCampaignRepository campaignRepository) {
    this.campaignRepository = campaignRepository;
  }

  /**
   * Create a new email campaign.
   *
   * @param campaign the campaign to create
   * @return the created campaign
   */
  public EmailCampaign createCampaign(EmailCampaign campaign) {
    // Set default status if not provided
    if (campaign.getStatus() == null) {
      campaign.setStatus("DRAFT");
    }

    campaign.setCreatedAt(LocalDateTime.now());
    campaign.setUpdatedAt(LocalDateTime.now());
    return campaignRepository.save(campaign);
  }

  /**
   * Find campaign by ID.
   *
   * @param id the campaign ID
   * @return the campaign
   * @throws ResourceNotFoundException if campaign not found
   */
  @Transactional(readOnly = true)
  public EmailCampaign findById(Long id) {
    return campaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmailCampaign", "id", id));
  }

  /**
   * Find campaign by ID and verify user ownership.
   *
   * @param id the campaign ID
   * @param userId the user ID
   * @return the campaign
   * @throws ResourceNotFoundException if campaign not found
   */
  @Transactional(readOnly = true)
  public EmailCampaign findByIdAndUserId(Long id, Long userId) {
    return campaignRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("EmailCampaign", "id", id));
  }

  /**
   * Find all campaigns for a user.
   *
   * @param userId the user ID
   * @return list of campaigns
   */
  @Transactional(readOnly = true)
  public List<EmailCampaign> findByUserId(Long userId) {
    return campaignRepository.findByUserId(userId);
  }

  /**
   * Find all campaigns for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of campaigns
   */
  @Transactional(readOnly = true)
  public Page<EmailCampaign> findByUserId(Long userId, Pageable pageable) {
    return campaignRepository.findByUserId(userId, pageable);
  }

  /**
   * Find campaigns by status.
   *
   * @param userId the user ID
   * @param status the campaign status
   * @param pageable pagination information
   * @return page of campaigns
   */
  @Transactional(readOnly = true)
  public Page<EmailCampaign> findByStatus(Long userId, String status, Pageable pageable) {
    return campaignRepository.findByUserIdAndStatus(userId, status, pageable);
  }

  /**
   * Search campaigns by name.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching campaigns
   */
  @Transactional(readOnly = true)
  public Page<EmailCampaign> searchByName(Long userId, String name, Pageable pageable) {
    return campaignRepository.findByUserIdAndNameContainingIgnoreCase(userId, name, pageable);
  }

  /**
   * Find scheduled campaigns ready to send.
   *
   * @return list of campaigns ready to send
   */
  @Transactional(readOnly = true)
  public List<EmailCampaign> findScheduledCampaignsReadyToSend() {
    return campaignRepository.findScheduledCampaignsReadyToSend("SCHEDULED", LocalDateTime.now());
  }

  /**
   * Update an existing campaign.
   *
   * @param id the campaign ID
   * @param userId the user ID
   * @param updatedCampaign the updated campaign data
   * @return the updated campaign
   * @throws ResourceNotFoundException if campaign not found
   * @throws ValidationException if campaign is not in editable state
   */
  public EmailCampaign updateCampaign(Long id, Long userId, EmailCampaign updatedCampaign) {
    EmailCampaign campaign = findByIdAndUserId(id, userId);

    // Only allow editing DRAFT campaigns
    if (!"DRAFT".equals(campaign.getStatus())) {
      throw new ValidationException("Can only edit campaigns in DRAFT status", "status");
    }

    if (updatedCampaign.getName() != null) {
      campaign.setName(updatedCampaign.getName());
    }
    if (updatedCampaign.getSubject() != null) {
      campaign.setSubject(updatedCampaign.getSubject());
    }
    if (updatedCampaign.getPreviewText() != null) {
      campaign.setPreviewText(updatedCampaign.getPreviewText());
    }
    if (updatedCampaign.getFromName() != null) {
      campaign.setFromName(updatedCampaign.getFromName());
    }
    if (updatedCampaign.getFromEmail() != null) {
      campaign.setFromEmail(updatedCampaign.getFromEmail());
    }
    if (updatedCampaign.getReplyToEmail() != null) {
      campaign.setReplyToEmail(updatedCampaign.getReplyToEmail());
    }

    campaign.setUpdatedAt(LocalDateTime.now());
    return campaignRepository.save(campaign);
  }

  /**
   * Update campaign status.
   *
   * @param id the campaign ID
   * @param userId the user ID
   * @param newStatus the new status
   * @return the updated campaign
   */
  public EmailCampaign updateStatus(Long id, Long userId, String newStatus) {
    EmailCampaign campaign = findByIdAndUserId(id, userId);
    campaign.setStatus(newStatus);
    campaign.setUpdatedAt(LocalDateTime.now());
    return campaignRepository.save(campaign);
  }

  /**
   * Schedule a campaign for future sending.
   *
   * @param id the campaign ID
   * @param userId the user ID
   * @param scheduledAt the scheduled time
   * @return the updated campaign
   */
  public EmailCampaign scheduleCampaign(Long id, Long userId, LocalDateTime scheduledAt) {
    EmailCampaign campaign = findByIdAndUserId(id, userId);

    if (scheduledAt.isBefore(LocalDateTime.now())) {
      throw new ValidationException("Scheduled time must be in the future", "scheduledAt");
    }

    campaign.setScheduledAt(scheduledAt);
    campaign.setStatus("SCHEDULED");
    campaign.setUpdatedAt(LocalDateTime.now());
    return campaignRepository.save(campaign);
  }

  /**
   * Delete a campaign.
   *
   * @param id the campaign ID
   * @param userId the user ID
   * @throws ResourceNotFoundException if campaign not found
   * @throws ValidationException if campaign is already sent
   */
  public void deleteCampaign(Long id, Long userId) {
    EmailCampaign campaign = findByIdAndUserId(id, userId);

    // Don't allow deletion of sent campaigns (for analytics)
    if ("SENT".equals(campaign.getStatus())) {
      throw new ValidationException("Cannot delete sent campaigns", "status");
    }

    campaignRepository.delete(campaign);
  }

  /**
   * Delete all campaigns for a user (GDPR compliance).
   *
   * @param userId the user ID
   */
  public void deleteAllByUserId(Long userId) {
    campaignRepository.deleteByUserId(userId);
  }

  /**
   * Count campaigns for a user.
   *
   * @param userId the user ID
   * @return count of campaigns
   */
  @Transactional(readOnly = true)
  public long countByUserId(Long userId) {
    return campaignRepository.countByUserId(userId);
  }

  /**
   * Count campaigns by status.
   *
   * @param userId the user ID
   * @param status the campaign status
   * @return count of campaigns with given status
   */
  @Transactional(readOnly = true)
  public long countByStatus(Long userId, String status) {
    return campaignRepository.countByUserIdAndStatus(userId, status);
  }
}
