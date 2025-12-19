package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.model.CampaignClick;
import com.openmailer.openmailer.repository.CampaignClickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for CampaignClick management operations.
 * Handles click tracking, analytics, and reporting.
 */
@Service
@Transactional
public class CampaignClickService {

  private final CampaignClickRepository clickRepository;

  @Autowired
  public CampaignClickService(CampaignClickRepository clickRepository) {
    this.clickRepository = clickRepository;
  }

  /**
   * Record a new click.
   *
   * @param click the click to record
   * @return the created click
   */
  public CampaignClick recordClick(CampaignClick click) {
    if (click.getClickedAt() == null) {
      click.setClickedAt(LocalDateTime.now());
    }
    return clickRepository.save(click);
  }

  /**
   * Find click by ID.
   *
   * @param id the ID (String)
   * @return the click
   * @throws ResourceNotFoundException if click not found
   */
  @Transactional(readOnly = true)
  public CampaignClick findById(String id) {
    return clickRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CampaignClick", "id", id));
  }

  /**
   * Find all clicks for a campaign.
   *
   * @param campaignId the ID (String)
   * @return list of clicks
   */
  @Transactional(readOnly = true)
  public List<CampaignClick> findByCampaign(String campaignId) {
    return clickRepository.findByCampaignId(campaignId);
  }

  /**
   * Find all clicks for a campaign with pagination.
   *
   * @param campaignId the ID (String)
   * @param pageable pagination information
   * @return page of clicks
   */
  @Transactional(readOnly = true)
  public Page<CampaignClick> findByCampaign(String campaignId, Pageable pageable) {
    return clickRepository.findByCampaignId(campaignId, pageable);
  }

  /**
   * Find all clicks for a recipient.
   *
   * @param recipientId the ID (String)
   * @return list of clicks
   */
  @Transactional(readOnly = true)
  public List<CampaignClick> findByRecipient(String recipientId) {
    return clickRepository.findByRecipientId(recipientId);
  }

  /**
   * Find all clicks for a link.
   *
   * @param linkId the ID (String)
   * @return list of clicks
   */
  @Transactional(readOnly = true)
  public List<CampaignClick> findByLink(String linkId) {
    return clickRepository.findByLinkId(linkId);
  }

  /**
   * Find all clicks for a link with pagination.
   *
   * @param linkId the ID (String)
   * @param pageable pagination information
   * @return page of clicks
   */
  @Transactional(readOnly = true)
  public Page<CampaignClick> findByLink(String linkId, Pageable pageable) {
    return clickRepository.findByLinkId(linkId, pageable);
  }

  /**
   * Find clicks within a date range.
   *
   * @param campaignId the ID (String)
   * @param startDate start date
   * @param endDate end date
   * @param pageable pagination information
   * @return page of clicks
   */
  @Transactional(readOnly = true)
  public Page<CampaignClick> findByDateRange(String campaignId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
    return clickRepository.findByDateRange(campaignId, startDate, endDate, pageable);
  }

  /**
   * Count clicks for a campaign.
   *
   * @param campaignId the ID (String)
   * @return count of clicks
   */
  @Transactional(readOnly = true)
  public long countByCampaign(String campaignId) {
    return clickRepository.countByCampaignId(campaignId);
  }

  /**
   * Count clicks for a link.
   *
   * @param linkId the ID (String)
   * @return count of clicks
   */
  @Transactional(readOnly = true)
  public long countByLink(String linkId) {
    return clickRepository.countByLinkId(linkId);
  }

  /**
   * Count clicks for a recipient.
   *
   * @param recipientId the ID (String)
   * @return count of clicks
   */
  @Transactional(readOnly = true)
  public long countByRecipient(String recipientId) {
    return clickRepository.countByRecipientId(recipientId);
  }

  /**
   * Count unique clickers for a campaign.
   *
   * @param campaignId the ID (String)
   * @return count of unique recipients
   */
  @Transactional(readOnly = true)
  public long countUniqueClickers(String campaignId) {
    return clickRepository.countUniqueClickersByCampaignId(campaignId);
  }

  /**
   * Count unique clickers for a link.
   *
   * @param linkId the ID (String)
   * @return count of unique recipients
   */
  @Transactional(readOnly = true)
  public long countUniqueClickersByLink(String linkId) {
    return clickRepository.countUniqueClickersByLinkId(linkId);
  }

  /**
   * Check if recipient has clicked a link.
   *
   * @param recipientId the ID (String)
   * @param linkId the ID (String)
   * @return true if clicked, false otherwise
   */
  @Transactional(readOnly = true)
  public boolean hasClicked(String recipientId, String linkId) {
    return clickRepository.existsByRecipientIdAndLinkId(recipientId, linkId);
  }

  /**
   * Delete all clicks for a campaign.
   *
   * @param campaignId the ID (String)
   */
  public void deleteAllByCampaign(String campaignId) {
    clickRepository.deleteByCampaignId(campaignId);
  }

  /**
   * Delete all clicks for a recipient.
   *
   * @param recipientId the ID (String)
   */
  public void deleteAllByRecipient(String recipientId) {
    clickRepository.deleteByRecipientId(recipientId);
  }
}
