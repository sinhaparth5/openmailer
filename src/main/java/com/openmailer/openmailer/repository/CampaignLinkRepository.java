package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.CampaignLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CampaignLink entity.
 * Provides CRUD operations and custom query methods for campaign link tracking.
 */
@Repository
public interface CampaignLinkRepository extends JpaRepository<CampaignLink, Long> {

  /**
   * Find all links for a campaign.
   *
   * @param campaignId the campaign ID
   * @return list of links
   */
  @Query("SELECT l FROM CampaignLink l WHERE l.campaign.id = :campaignId")
  List<CampaignLink> findByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Find all links for a campaign with pagination.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of links
   */
  @Query("SELECT l FROM CampaignLink l WHERE l.campaign.id = :campaignId")
  Page<CampaignLink> findByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

  /**
   * Find link by short code.
   *
   * @param shortCode the short code
   * @return Optional containing the link if found
   */
  Optional<CampaignLink> findByShortCode(String shortCode);

  /**
   * Find link by campaign and original URL.
   *
   * @param campaignId the campaign ID
   * @param originalUrl the original URL
   * @return Optional containing the link if found
   */
  @Query("SELECT l FROM CampaignLink l WHERE l.campaign.id = :campaignId AND l.originalUrl = :originalUrl")
  Optional<CampaignLink> findByCampaignIdAndOriginalUrl(@Param("campaignId") Long campaignId, @Param("originalUrl") String originalUrl);

  /**
   * Find most clicked links for a campaign.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of links ordered by click count
   */
  @Query("SELECT l FROM CampaignLink l WHERE l.campaign.id = :campaignId ORDER BY l.clickCount DESC")
  Page<CampaignLink> findTopClickedByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

  /**
   * Count links for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of links
   */
  @Query("SELECT COUNT(l) FROM CampaignLink l WHERE l.campaign.id = :campaignId")
  long countByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Get total clicks for a campaign.
   *
   * @param campaignId the campaign ID
   * @return total click count
   */
  @Query("SELECT SUM(l.clickCount) FROM CampaignLink l WHERE l.campaign.id = :campaignId")
  Long getTotalClicksByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Get total unique clicks for a campaign.
   *
   * @param campaignId the campaign ID
   * @return total unique click count
   */
  @Query("SELECT SUM(l.uniqueClickCount) FROM CampaignLink l WHERE l.campaign.id = :campaignId")
  Long getTotalUniqueClicksByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Check if short code exists.
   *
   * @param shortCode the short code
   * @return true if exists, false otherwise
   */
  boolean existsByShortCode(String shortCode);

  /**
   * Delete all links for a campaign.
   *
   * @param campaignId the campaign ID
   */
  @Query("DELETE FROM CampaignLink l WHERE l.campaign.id = :campaignId")
  void deleteByCampaignId(@Param("campaignId") Long campaignId);
}
