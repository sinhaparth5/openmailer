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
   * @param id the ID (String)
   * @return the campaign
   * @throws ResourceNotFoundException if campaign not found
   */
  @Transactional(readOnly = true)
  public EmailCampaign findById(String id) {
    return campaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmailCampaign", "id", id));
  }

  /**
   * Find campaign by ID and verify user ownership.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @return the campaign
   * @throws ResourceNotFoundException if campaign not found
   */
  @Transactional(readOnly = true)
  public EmailCampaign findByIdAndUserId(String id, String userId) {
    return campaignRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("EmailCampaign", "id", id));
  }

  /**
   * Find all campaigns for a user.
   *
   * @param userId the ID (String)
   * @return list of campaigns
   */
  @Transactional(readOnly = true)
  public List<EmailCampaign> findByUserId(String userId) {
    return campaignRepository.findByUserId(userId);
  }

  /**
   * Find all campaigns for a user with pagination.
   *
   * @param userId the ID (String)
   * @param pageable pagination information
   * @return page of campaigns
   */
  @Transactional(readOnly = true)
  public Page<EmailCampaign> findByUserId(String userId, Pageable pageable) {
    return campaignRepository.findByUserId(userId, pageable);
  }

  /**
   * Find campaigns by status.
   *
   * @param userId the ID (String)
   * @param status the campaign status
   * @param pageable pagination information
   * @return page of campaigns
   */
  @Transactional(readOnly = true)
  public Page<EmailCampaign> findByStatus(String userId, String status, Pageable pageable) {
    return campaignRepository.findByUserIdAndStatus(userId, status, pageable);
  }

  /**
   * Search campaigns by name.
   *
   * @param userId the ID (String)
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching campaigns
   */
  @Transactional(readOnly = true)
  public Page<EmailCampaign> searchByName(String userId, String name, Pageable pageable) {
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
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param updatedCampaign the updated campaign data
   * @return the updated campaign
   * @throws ResourceNotFoundException if campaign not found
   * @throws ValidationException if campaign is not in editable state
   */
  public EmailCampaign updateCampaign(String id, String userId, EmailCampaign updatedCampaign) {
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
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param newStatus the new status
   * @return the updated campaign
   */
  public EmailCampaign updateStatus(String id, String userId, String newStatus) {
    EmailCampaign campaign = findByIdAndUserId(id, userId);
    campaign.setStatus(newStatus);
    campaign.setUpdatedAt(LocalDateTime.now());
    return campaignRepository.save(campaign);
  }

  /**
   * Schedule a campaign for future sending.
   *
   * @param id the ID (String)
   * @param userId the ID (String)
   * @param scheduledAt the scheduled time
   * @return the updated campaign
   */
  public EmailCampaign scheduleCampaign(String id, String userId, LocalDateTime scheduledAt) {
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
   * @param id the ID (String)
   * @param userId the ID (String)
   * @throws ResourceNotFoundException if campaign not found
   * @throws ValidationException if campaign is already sent
   */
  public void deleteCampaign(String id, String userId) {
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
   * @param userId the ID (String)
   */
  public void deleteAllByUserId(String userId) {
    campaignRepository.deleteByUserId(userId);
  }

  /**
   * Count campaigns for a user.
   *
   * @param userId the ID (String)
   * @return count of campaigns
   */
  @Transactional(readOnly = true)
  public long countByUserId(String userId) {
    return campaignRepository.countByUserId(userId);
  }

  /**
   * Count campaigns by status.
   *
   * @param userId the ID (String)
   * @param status the campaign status
   * @return count of campaigns with given status
   */
  @Transactional(readOnly = true)
  public long countByStatus(String userId, String status) {
    return campaignRepository.countByUserIdAndStatus(userId, status);
  }
}
