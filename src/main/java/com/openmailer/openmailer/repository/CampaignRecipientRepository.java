package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.CampaignRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CampaignRecipient entity.
 * Provides CRUD operations and custom query methods for campaign recipient tracking.
 */
@Repository
public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, Long> {

  /**
   * Find all recipients for a campaign.
   *
   * @param campaignId the campaign ID
   * @return list of recipients
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId")
  List<CampaignRecipient> findByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Find all recipients for a campaign with pagination.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId")
  Page<CampaignRecipient> findByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

  /**
   * Find recipients by campaign and status.
   *
   * @param campaignId the campaign ID
   * @param status the recipient status
   * @param pageable pagination information
   * @return page of recipients
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.status = :status")
  Page<CampaignRecipient> findByCampaignIdAndStatus(@Param("campaignId") Long campaignId, @Param("status") String status, Pageable pageable);

  /**
   * Find all recipients for a contact.
   *
   * @param contactId the contact ID
   * @return list of recipients
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.contact.id = :contactId")
  List<CampaignRecipient> findByContactId(@Param("contactId") Long contactId);

  /**
   * Find recipient by campaign and contact.
   *
   * @param campaignId the campaign ID
   * @param contactId the contact ID
   * @return Optional containing the recipient if found
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.contact.id = :contactId")
  Optional<CampaignRecipient> findByCampaignIdAndContactId(@Param("campaignId") Long campaignId, @Param("contactId") Long contactId);

  /**
   * Find recipient by tracking ID.
   *
   * @param trackingId the tracking ID
   * @return Optional containing the recipient if found
   */
  Optional<CampaignRecipient> findByTrackingId(String trackingId);

  /**
   * Find recipients that opened emails.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients who opened
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.openedAt IS NOT NULL")
  Page<CampaignRecipient> findOpenedByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

  /**
   * Find recipients that clicked links.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients who clicked
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.clickedAt IS NOT NULL")
  Page<CampaignRecipient> findClickedByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

  /**
   * Find recipients that bounced.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of recipients who bounced
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.bouncedAt IS NOT NULL")
  Page<CampaignRecipient> findBouncedByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

  /**
   * Count recipients by campaign.
   *
   * @param campaignId the campaign ID
   * @return count of recipients
   */
  @Query("SELECT COUNT(r) FROM CampaignRecipient r WHERE r.campaign.id = :campaignId")
  long countByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Count recipients by campaign and status.
   *
   * @param campaignId the campaign ID
   * @param status the recipient status
   * @return count of recipients
   */
  @Query("SELECT COUNT(r) FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.status = :status")
  long countByCampaignIdAndStatus(@Param("campaignId") Long campaignId, @Param("status") String status);

  /**
   * Count opened emails for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of opened emails
   */
  @Query("SELECT COUNT(r) FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.openedAt IS NOT NULL")
  long countOpenedByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Count clicked emails for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of clicked emails
   */
  @Query("SELECT COUNT(r) FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.clickedAt IS NOT NULL")
  long countClickedByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Count bounced emails for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of bounced emails
   */
  @Query("SELECT COUNT(r) FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.bouncedAt IS NOT NULL")
  long countBouncedByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Find pending recipients for a campaign (for retry logic).
   *
   * @param campaignId the campaign ID
   * @param maxRetries maximum retry attempts
   * @return list of pending recipients
   */
  @Query("SELECT r FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.status = 'PENDING' AND r.retryCount < :maxRetries")
  List<CampaignRecipient> findPendingRecipients(@Param("campaignId") Long campaignId, @Param("maxRetries") Integer maxRetries);

  /**
   * Delete all recipients for a campaign.
   *
   * @param campaignId the campaign ID
   */
  @Query("DELETE FROM CampaignRecipient r WHERE r.campaign.id = :campaignId")
  void deleteByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Check if recipient exists for campaign and contact.
   *
   * @param campaignId the campaign ID
   * @param contactId the contact ID
   * @return true if exists, false otherwise
   */
  @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM CampaignRecipient r WHERE r.campaign.id = :campaignId AND r.contact.id = :contactId")
  boolean existsByCampaignIdAndContactId(@Param("campaignId") Long campaignId, @Param("contactId") Long contactId);
}
