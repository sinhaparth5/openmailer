package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.model.CampaignClick;
import com.openmailer.openmailer.model.CampaignLink;
import com.openmailer.openmailer.model.CampaignRecipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for handling email tracking functionality.
 * Generates tracking IDs for opens and orchestrates tracking operations.
 */
@Service
@Transactional
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final CampaignRecipientService recipientService;
    private final CampaignLinkService linkService;
    private final CampaignClickService clickService;

    @Autowired
    public TrackingService(
            CampaignRecipientService recipientService,
            CampaignLinkService linkService,
            CampaignClickService clickService) {
        this.recipientService = recipientService;
        this.linkService = linkService;
        this.clickService = clickService;
    }

    /**
     * Generates a unique tracking ID for a recipient.
     * This ID is used in tracking pixels to identify when emails are opened.
     *
     * @return unique tracking ID
     */
    public String generateTrackingId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Records an email open event.
     * This is called when the tracking pixel is loaded.
     *
     * @param trackingId the tracking ID from the pixel URL
     * @return the updated recipient
     */
    public CampaignRecipient recordOpen(String trackingId) {
        log.info("Recording email open for tracking ID: {}", trackingId);

        try {
            CampaignRecipient recipient = recipientService.trackOpen(trackingId);
            log.info("Email opened - Campaign: {}, Contact: {}, Open count: {}",
                    recipient.getCampaign().getId(),
                    recipient.getContact().getEmail(),
                    recipient.getOpenCount());

            return recipient;
        } catch (Exception e) {
            log.error("Failed to record email open for tracking ID {}: {}",
                    trackingId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Records a link click event.
     * This is called when a tracked link is clicked.
     *
     * @param shortCode the short code from the tracking link
     * @param trackingId the recipient's tracking ID (from query param or session)
     * @return the campaign link that was clicked
     */
    public CampaignLink recordClick(String shortCode, String trackingId) {
        log.info("Recording link click - Short code: {}, Tracking ID: {}", shortCode, trackingId);

        try {
            // Get the link
            CampaignLink link = linkService.findByShortCode(shortCode);

            // Get the recipient
            CampaignRecipient recipient = recipientService.findByTrackingId(trackingId);

            // Check if this is a unique click (first time this recipient clicked this link)
            boolean isUniqueClick = !clickService.hasClicked(recipient.getId(), link.getId());

            // Increment click count on the link
            linkService.incrementClickCount(shortCode, isUniqueClick);

            // Track click on recipient
            recipientService.trackClick(trackingId);

            // Record detailed click event
            CampaignClick click = new CampaignClick();
            click.setCampaign(recipient.getCampaign());
            click.setRecipient(recipient);
            click.setLink(link);
            clickService.recordClick(click);

            log.info("Link clicked - Campaign: {}, Contact: {}, URL: {}, Unique: {}",
                    recipient.getCampaign().getId(),
                    recipient.getContact().getEmail(),
                    link.getOriginalUrl(),
                    isUniqueClick);

            return link;
        } catch (Exception e) {
            log.error("Failed to record link click for short code {}: {}",
                    shortCode, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Records a click without tracking ID (anonymous click).
     * Only increments the link click count.
     *
     * @param shortCode the short code from the tracking link
     * @return the campaign link that was clicked
     */
    public CampaignLink recordAnonymousClick(String shortCode) {
        log.info("Recording anonymous link click for short code: {}", shortCode);

        try {
            // Increment click count (not unique since we don't know if it's a repeat)
            CampaignLink link = linkService.incrementClickCount(shortCode, false);

            log.info("Anonymous click recorded - URL: {}", link.getOriginalUrl());

            return link;
        } catch (Exception e) {
            log.error("Failed to record anonymous click for short code {}: {}",
                    shortCode, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generates or retrieves a tracking link for a URL in a campaign.
     *
     * @param campaignId the campaign ID
     * @param originalUrl the original URL to track
     * @param baseUrl the base URL for the tracking service
     * @return the tracking URL
     */
    public String generateTrackingUrl(String campaignId, String originalUrl, String baseUrl) {
        CampaignLink link = linkService.findOrCreateLink(campaignId, originalUrl);
        return baseUrl + "/track/click/" + link.getShortCode();
    }

    /**
     * Generates the tracking pixel URL for a recipient.
     *
     * @param trackingId the recipient's tracking ID
     * @param baseUrl the base URL for the tracking service
     * @return the tracking pixel URL
     */
    public String generateTrackingPixelUrl(String trackingId, String baseUrl) {
        return baseUrl + "/track/open/" + trackingId;
    }
}
