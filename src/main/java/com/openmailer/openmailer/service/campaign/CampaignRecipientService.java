package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.CampaignRecipient;
import com.openmailer.openmailer.repository.CampaignRecipientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for CampaignRecipient management operations.
 * Handles recipient tracking, status updates, and analytics.
 */
@Service
@Transactional
public class CampaignRecipientService {

  private final CampaignRecipientRepository recipientRepository;

  @Autowired
  public CampaignRecipientService(CampaignRecipientRepository recipientRepository) {
    this.recipientRepository = recipientRepository;
  }

  /**
   * Create a new campaign recipient.
   *
   * @param recipient the recipient to create
   * @return the created recipient
   * @throws ValidationException if recipient already exists for campaign and contact
   */
  public CampaignRecipient createRecipient(CampaignRecipient recipient) {
    // Check if recipient already exists
    Long campaignId = recipient.getCampaign().getId();
    Long contactId = recipient.getContact().getId();
    if (recipientRepository.existsByCampaignIdAndContactId(campaignId, contactId)) {
      throw new ValidationException("Recipient already exists for this campaign and contact", "recipient");
    }

    // Set defaults
    if (recipient.getStatus() == null) {
      recipient.setStatus("PENDING");
    }
    if (recipient.getOpenCount() == null) {
      recipient.setOpenCount(0);
    }
    if (recipient.getClickCount() == null) {
      recipient.setClickCount(0);
    }
    if (recipient.getRetryCount() == null) {
      recipient.setRetryCount(0);
    }

    return recipientRepository.save(recipient);
  }

  /**
   * Find recipient by ID.
   *
   * @param id the recipient ID
   * @return the recipient
   * @throws ResourceNotFoundException if recipient not found
   */
  @Transactional(readOnly = true)
  public CampaignRecipient findById(Long id) {
    return recipientRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CampaignRecipient", "id", id));
  }

  /**
   * Find recipient by tracking ID.
   *
   * @param trackingId the tracking ID
   * @return the recipient
   * @throws ResourceNotFoundException if recipient not found
   */
  @Transactional(readOnly = true)
  public CampaignRecipient findByTrackingId(String trackingId) {
    return recipientRepository.findByTrackingId(trackingId)
        .orElseThrow(() -> new ResourceNotFoundException("CampaignRecipient", "trackingId", trackingId));
  }

  /**
   * Find recipient by campaign and contact.
   *
   * @param campaignId the campaign ID
   * @param contactId the contact ID
   * @return the recipient
   * @throws ResourceNotFoundException if recipient not found
   */
  @Transactional(readOnly = true)
  public CampaignRecipient findByCampaignAndContact(Long campaignId, Long contactId) {
    return recipientRepository.findByCampaignIdAndContactId(campaignId, contactId)
        .orElseThrow(() -> new ResourceNotFoundException("CampaignRecipient not found"));
  }

  /**
   * Find all recipients for a campaign.
   *
   * @param campaignId the campaign ID
   * @return list of recipients
   */
  @Transactional(readOnly = true)
  public List<CampaignRecipient> findByCampaign(Long campaignId) {
    return recipientRepository.findByCampaignId(campaignId);
  }

  /**
   * Find all recipients for a campaign with pagination.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients
   */
  @Transactional(readOnly = true)
  public Page<CampaignRecipient> findByCampaign(Long campaignId, Pageable pageable) {
    return recipientRepository.findByCampaignId(campaignId, pageable);
  }

  /**
   * Find recipients by status.
   *
   * @param campaignId the campaign ID
   * @param status the recipient status
   * @param pageable pagination information
   * @return page of recipients
   */
  @Transactional(readOnly = true)
  public Page<CampaignRecipient> findByStatus(Long campaignId, String status, Pageable pageable) {
    return recipientRepository.findByCampaignIdAndStatus(campaignId, status, pageable);
  }

  /**
   * Find all recipients for a contact.
   *
   * @param contactId the contact ID
   * @return list of recipients
   */
  @Transactional(readOnly = true)
  public List<CampaignRecipient> findByContact(Long contactId) {
    return recipientRepository.findByContactId(contactId);
  }

  /**
   * Find recipients who opened emails.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients who opened
   */
  @Transactional(readOnly = true)
  public Page<CampaignRecipient> findOpened(Long campaignId, Pageable pageable) {
    return recipientRepository.findOpenedByCampaignId(campaignId, pageable);
  }

  /**
   * Find recipients who clicked links.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients who clicked
   */
  @Transactional(readOnly = true)
  public Page<CampaignRecipient> findClicked(Long campaignId, Pageable pageable) {
    return recipientRepository.findClickedByCampaignId(campaignId, pageable);
  }

  /**
   * Find recipients who bounced.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients who bounced
   */
  @Transactional(readOnly = true)
  public Page<CampaignRecipient> findBounced(Long campaignId, Pageable pageable) {
    return recipientRepository.findBouncedByCampaignId(campaignId, pageable);
  }

  /**
   * Mark recipient as sent.
   *
   * @param id the recipient ID
   * @return the updated recipient
   */
  public CampaignRecipient markAsSent(Long id) {
    CampaignRecipient recipient = findById(id);
    recipient.setStatus("SENT");
    recipient.setSentAt(LocalDateTime.now());
    return recipientRepository.save(recipient);
  }

  /**
   * Mark recipient as delivered.
   *
   * @param id the recipient ID
   * @return the updated recipient
   */
  public CampaignRecipient markAsDelivered(Long id) {
    CampaignRecipient recipient = findById(id);
    recipient.setStatus("DELIVERED");
    recipient.setDeliveredAt(LocalDateTime.now());
    return recipientRepository.save(recipient);
  }

  /**
   * Track email open.
   *
   * @param trackingId the tracking ID
   * @return the updated recipient
   */
  public CampaignRecipient trackOpen(String trackingId) {
    CampaignRecipient recipient = findByTrackingId(trackingId);

    // First open
    if (recipient.getOpenedAt() == null) {
      recipient.setOpenedAt(LocalDateTime.now());
    }

    recipient.setOpenCount(recipient.getOpenCount() + 1);
    return recipientRepository.save(recipient);
  }

  /**
   * Track link click.
   *
   * @param trackingId the tracking ID
   * @return the updated recipient
   */
  public CampaignRecipient trackClick(String trackingId) {
    CampaignRecipient recipient = findByTrackingId(trackingId);

    // First click
    if (recipient.getClickedAt() == null) {
      recipient.setClickedAt(LocalDateTime.now());
    }

    recipient.setClickCount(recipient.getClickCount() + 1);
    return recipientRepository.save(recipient);
  }

  /**
   * Mark recipient as bounced.
   *
   * @param id the recipient ID
   * @param errorMessage the error message
   * @return the updated recipient
   */
  public CampaignRecipient markAsBounced(Long id, String errorMessage) {
    CampaignRecipient recipient = findById(id);
    recipient.setStatus("BOUNCED");
    recipient.setBouncedAt(LocalDateTime.now());
    recipient.setErrorMessage(errorMessage);
    return recipientRepository.save(recipient);
  }

  /**
   * Mark recipient as complained.
   *
   * @param id the recipient ID
   * @return the updated recipient
   */
  public CampaignRecipient markAsComplained(Long id) {
    CampaignRecipient recipient = findById(id);
    recipient.setStatus("COMPLAINED");
    recipient.setComplainedAt(LocalDateTime.now());
    return recipientRepository.save(recipient);
  }

  /**
   * Mark recipient as failed.
   *
   * @param id the recipient ID
   * @param errorMessage the error message
   * @return the updated recipient
   */
  public CampaignRecipient markAsFailed(Long id, String errorMessage) {
    CampaignRecipient recipient = findById(id);
    recipient.setStatus("FAILED");
    recipient.setErrorMessage(errorMessage);
    recipient.setRetryCount(recipient.getRetryCount() + 1);
    return recipientRepository.save(recipient);
  }

  /**
   * Update recipient status.
   *
   * @param id the recipient ID
   * @param status the new status
   * @return the updated recipient
   */
  public CampaignRecipient updateStatus(Long id, String status) {
    CampaignRecipient recipient = findById(id);
    recipient.setStatus(status);
    return recipientRepository.save(recipient);
  }

  /**
   * Get pending recipients for retry.
   *
   * @param campaignId the campaign ID
   * @param maxRetries maximum retry attempts
   * @return list of pending recipients
   */
  @Transactional(readOnly = true)
  public List<CampaignRecipient> getPendingRecipients(Long campaignId, Integer maxRetries) {
    return recipientRepository.findPendingRecipients(campaignId, maxRetries);
  }

  /**
   * Count recipients for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of recipients
   */
  @Transactional(readOnly = true)
  public long countByCampaign(Long campaignId) {
    return recipientRepository.countByCampaignId(campaignId);
  }

  /**
   * Count recipients by status.
   *
   * @param campaignId the campaign ID
   * @param status the recipient status
   * @return count of recipients
   */
  @Transactional(readOnly = true)
  public long countByStatus(Long campaignId, String status) {
    return recipientRepository.countByCampaignIdAndStatus(campaignId, status);
  }

  /**
   * Count opened emails.
   *
   * @param campaignId the campaign ID
   * @return count of opened emails
   */
  @Transactional(readOnly = true)
  public long countOpened(Long campaignId) {
    return recipientRepository.countOpenedByCampaignId(campaignId);
  }

  /**
   * Count clicked emails.
   *
   * @param campaignId the campaign ID
   * @return count of clicked emails
   */
  @Transactional(readOnly = true)
  public long countClicked(Long campaignId) {
    return recipientRepository.countClickedByCampaignId(campaignId);
  }

  /**
   * Count bounced emails.
   *
   * @param campaignId the campaign ID
   * @return count of bounced emails
   */
  @Transactional(readOnly = true)
  public long countBounced(Long campaignId) {
    return recipientRepository.countBouncedByCampaignId(campaignId);
  }

  /**
   * Delete all recipients for a campaign.
   *
   * @param campaignId the campaign ID
   */
  public void deleteAllByCampaign(Long campaignId) {
    recipientRepository.deleteByCampaignId(campaignId);
  }
}
