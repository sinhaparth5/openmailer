package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.CampaignClick;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for CampaignClick entity.
 * Provides CRUD operations and custom query methods for click tracking and analytics.
 */
@Repository
public interface CampaignClickRepository extends JpaRepository<CampaignClick, String> {

  /**
   * Find all clicks for a campaign.
   *
   * @param campaignId the campaign ID
   * @return list of clicks
   */
  @Query("SELECT c FROM CampaignClick c WHERE c.campaign.id = :campaignId")
  List<CampaignClick> findByCampaignId(@Param("campaignId") String campaignId);

  /**
   * Find all clicks for a campaign with pagination.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of clicks
   */
  @Query("SELECT c FROM CampaignClick c WHERE c.campaign.id = :campaignId")
  Page<CampaignClick> findByCampaignId(@Param("campaignId") String campaignId, Pageable pageable);

  /**
   * Find all clicks for a recipient.
   *
   * @param recipientId the recipient ID
   * @return list of clicks
   */
  @Query("SELECT c FROM CampaignClick c WHERE c.recipient.id = :recipientId")
  List<CampaignClick> findByRecipientId(@Param("recipientId") String recipientId);

  /**
   * Find all clicks for a link.
   *
   * @param linkId the link ID
   * @return list of clicks
   */
  @Query("SELECT c FROM CampaignClick c WHERE c.link.id = :linkId")
  List<CampaignClick> findByLinkId(@Param("linkId") String linkId);

  /**
   * Find all clicks for a link with pagination.
   *
   * @param linkId the link ID
   * @param pageable pagination information
   * @return page of clicks
   */
  @Query("SELECT c FROM CampaignClick c WHERE c.link.id = :linkId")
  Page<CampaignClick> findByLinkId(@Param("linkId") String linkId, Pageable pageable);

  /**
   * Find clicks within a time range.
   *
   * @param campaignId the campaign ID
   * @param startDate start date
   * @param endDate end date
   * @param pageable pagination information
   * @return page of clicks
   */
  @Query("SELECT c FROM CampaignClick c WHERE c.campaign.id = :campaignId AND c.clickedAt BETWEEN :startDate AND :endDate")
  Page<CampaignClick> findByDateRange(@Param("campaignId") String campaignId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

  /**
   * Count clicks for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of clicks
   */
  @Query("SELECT COUNT(c) FROM CampaignClick c WHERE c.campaign.id = :campaignId")
  long countByCampaignId(@Param("campaignId") String campaignId);

  /**
   * Count clicks for a link.
   *
   * @param linkId the link ID
   * @return count of clicks
   */
  @Query("SELECT COUNT(c) FROM CampaignClick c WHERE c.link.id = :linkId")
  long countByLinkId(@Param("linkId") String linkId);

  /**
   * Count clicks for a recipient.
   *
   * @param recipientId the recipient ID
   * @return count of clicks
   */
  @Query("SELECT COUNT(c) FROM CampaignClick c WHERE c.recipient.id = :recipientId")
  long countByRecipientId(@Param("recipientId") String recipientId);

  /**
   * Count unique clickers for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of unique recipients who clicked
   */
  @Query("SELECT COUNT(DISTINCT c.recipient.id) FROM CampaignClick c WHERE c.campaign.id = :campaignId")
  long countUniqueClickersByCampaignId(@Param("campaignId") String campaignId);

  /**
   * Count unique clickers for a link.
   *
   * @param linkId the link ID
   * @return count of unique recipients who clicked
   */
  @Query("SELECT COUNT(DISTINCT c.recipient.id) FROM CampaignClick c WHERE c.link.id = :linkId")
  long countUniqueClickersByLinkId(@Param("linkId") String linkId);

  /**
   * Check if recipient has clicked a link.
   *
   * @param recipientId the recipient ID
   * @param linkId the link ID
   * @return true if clicked, false otherwise
   */
  @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CampaignClick c WHERE c.recipient.id = :recipientId AND c.link.id = :linkId")
  boolean existsByRecipientIdAndLinkId(@Param("recipientId") String recipientId, @Param("linkId") String linkId);

  /**
   * Delete all clicks for a campaign.
   *
   * @param campaignId the campaign ID
   */
  @Query("DELETE FROM CampaignClick c WHERE c.campaign.id = :campaignId")
  void deleteByCampaignId(@Param("campaignId") String campaignId);

  /**
   * Delete all clicks for a recipient.
   *
   * @param recipientId the recipient ID
   */
  @Query("DELETE FROM CampaignClick c WHERE c.recipient.id = :recipientId")
  void deleteByRecipientId(@Param("recipientId") String recipientId);
}
