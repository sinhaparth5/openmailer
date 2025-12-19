package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.CampaignLink;
import com.openmailer.openmailer.repository.CampaignLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service class for CampaignLink management operations.
 * Handles link tracking, short code generation, and click analytics.
 */
@Service
@Transactional
public class CampaignLinkService {

  private final CampaignLinkRepository linkRepository;

  @Autowired
  public CampaignLinkService(CampaignLinkRepository linkRepository) {
    this.linkRepository = linkRepository;
  }

  /**
   * Create a new campaign link.
   *
   * @param link the link to create
   * @return the created link
   */
  public CampaignLink createLink(CampaignLink link) {
    // Generate short code if not provided
    if (link.getShortCode() == null || link.getShortCode().isEmpty()) {
      link.setShortCode(generateUniqueShortCode());
    }

    // Validate short code uniqueness
    if (linkRepository.existsByShortCode(link.getShortCode())) {
      throw new ValidationException("Short code already exists", "shortCode");
    }

    // Set defaults
    if (link.getClickCount() == null) {
      link.setClickCount(0);
    }
    if (link.getUniqueClickCount() == null) {
      link.setUniqueClickCount(0);
    }

    return linkRepository.save(link);
  }

  /**
   * Find or create link for a campaign and URL.
   *
   * @param campaignId the ID (String)
   * @param originalUrl the original URL
   * @return the link
   */
  public CampaignLink findOrCreateLink(String campaignId, String originalUrl) {
    return linkRepository.findByCampaignIdAndOriginalUrl(campaignId, originalUrl)
        .orElseGet(() -> {
          CampaignLink newLink = new CampaignLink();
          newLink.getCampaign().setId(campaignId);
          newLink.setOriginalUrl(originalUrl);
          newLink.setShortCode(generateUniqueShortCode());
          return createLink(newLink);
        });
  }

  /**
   * Find link by ID.
   *
   * @param id the ID (String)
   * @return the link
   * @throws ResourceNotFoundException if link not found
   */
  @Transactional(readOnly = true)
  public CampaignLink findById(String id) {
    return linkRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CampaignLink", "id", id));
  }

  /**
   * Find link by short code.
   *
   * @param shortCode the short code
   * @return the link
   * @throws ResourceNotFoundException if link not found
   */
  @Transactional(readOnly = true)
  public CampaignLink findByShortCode(String shortCode) {
    return linkRepository.findByShortCode(shortCode)
        .orElseThrow(() -> new ResourceNotFoundException("CampaignLink", "shortCode", shortCode));
  }

  /**
   * Find all links for a campaign.
   *
   * @param campaignId the ID (String)
   * @return list of links
   */
  @Transactional(readOnly = true)
  public List<CampaignLink> findByCampaign(String campaignId) {
    return linkRepository.findByCampaignId(campaignId);
  }

  /**
   * Find all links for a campaign with pagination.
   *
   * @param campaignId the ID (String)
   * @param pageable pagination information
   * @return page of links
   */
  @Transactional(readOnly = true)
  public Page<CampaignLink> findByCampaign(String campaignId, Pageable pageable) {
    return linkRepository.findByCampaignId(campaignId, pageable);
  }

  /**
   * Find top clicked links for a campaign.
   *
   * @param campaignId the ID (String)
   * @param pageable pagination information
   * @return page of top clicked links
   */
  @Transactional(readOnly = true)
  public Page<CampaignLink> findTopClicked(String campaignId, Pageable pageable) {
    return linkRepository.findTopClickedByCampaignId(campaignId, pageable);
  }

  /**
   * Increment click count for a link.
   *
   * @param shortCode the short code
   * @param isUnique whether this is a unique click
   * @return the updated link
   */
  public CampaignLink incrementClickCount(String shortCode, boolean isUnique) {
    CampaignLink link = findByShortCode(shortCode);
    link.setClickCount(link.getClickCount() + 1);

    if (isUnique) {
      link.setUniqueClickCount(link.getUniqueClickCount() + 1);
    }

    return linkRepository.save(link);
  }

  /**
   * Get total clicks for a campaign.
   *
   * @param campaignId the ID (String)
   * @return total click count
   */
  @Transactional(readOnly = true)
  public long getTotalClicks(String campaignId) {
    Long total = linkRepository.getTotalClicksByCampaignId(campaignId);
    return total != null ? total : 0L;
  }

  /**
   * Get total unique clicks for a campaign.
   *
   * @param campaignId the ID (String)
   * @return total unique click count
   */
  @Transactional(readOnly = true)
  public long getTotalUniqueClicks(String campaignId) {
    Long total = linkRepository.getTotalUniqueClicksByCampaignId(campaignId);
    return total != null ? total : 0L;
  }

  /**
   * Count links for a campaign.
   *
   * @param campaignId the ID (String)
   * @return count of links
   */
  @Transactional(readOnly = true)
  public long countByCampaign(String campaignId) {
    return linkRepository.countByCampaignId(campaignId);
  }

  /**
   * Delete all links for a campaign.
   *
   * @param campaignId the ID (String)
   */
  public void deleteAllByCampaign(String campaignId) {
    linkRepository.deleteByCampaignId(campaignId);
  }

  /**
   * Generate a unique short code.
   *
   * @return unique short code
   */
  private String generateUniqueShortCode() {
    String shortCode;
    do {
      shortCode = UUID.randomUUID().toString().substring(0, 8);
    } while (linkRepository.existsByShortCode(shortCode));
    return shortCode;
  }
}
