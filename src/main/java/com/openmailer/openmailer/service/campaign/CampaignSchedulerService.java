package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.repository.EmailCampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for scheduling and automatically sending campaigns.
 * Runs a cron job to check for campaigns that are scheduled to be sent.
 */
@Service
@Transactional
public class CampaignSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CampaignSchedulerService.class);

    private final EmailCampaignRepository campaignRepository;
    private final CampaignSendingService sendingService;

    @Autowired
    public CampaignSchedulerService(
            EmailCampaignRepository campaignRepository,
            CampaignSendingService sendingService) {
        this.campaignRepository = campaignRepository;
        this.sendingService = sendingService;
    }

    /**
     * Checks for scheduled campaigns and sends them if their scheduled time has arrived.
     * Runs every minute to check for campaigns to send.
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at 0 seconds
    public void processScheduledCampaigns() {
        log.debug("Checking for scheduled campaigns to send");

        try {
            LocalDateTime now = LocalDateTime.now();

            // Find campaigns that are scheduled to be sent
            List<EmailCampaign> scheduledCampaigns = campaignRepository.findScheduledCampaigns(now);

            if (scheduledCampaigns.isEmpty()) {
                log.debug("No scheduled campaigns found");
                return;
            }

            log.info("Found {} scheduled campaigns to process", scheduledCampaigns.size());

            for (EmailCampaign campaign : scheduledCampaigns) {
                try {
                    processCampaign(campaign);
                } catch (Exception e) {
                    log.error("Error processing scheduled campaign {}: {}",
                            campaign.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error in scheduled campaign processor: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single scheduled campaign.
     *
     * @param campaign the campaign to process
     */
    private void processCampaign(EmailCampaign campaign) {
        log.info("Processing scheduled campaign: {} (scheduled for: {})",
                campaign.getId(), campaign.getScheduledAt());

        // Validate campaign is ready to send
        if (!isReadyToSend(campaign)) {
            log.warn("Campaign {} is not ready to send, skipping", campaign.getId());
            campaign.setStatus("FAILED");
            campaign.setUpdatedAt(LocalDateTime.now());
            campaignRepository.save(campaign);
            return;
        }

        // Trigger async sending
        try {
            sendingService.sendCampaignAsync(campaign.getId());
            log.info("Campaign {} has been queued for sending", campaign.getId());
        } catch (Exception e) {
            log.error("Failed to queue campaign {} for sending: {}",
                    campaign.getId(), e.getMessage(), e);
            campaign.setStatus("FAILED");
            campaign.setUpdatedAt(LocalDateTime.now());
            campaignRepository.save(campaign);
        }
    }

    /**
     * Validates that a campaign is ready to be sent.
     *
     * @param campaign the campaign to validate
     * @return true if ready, false otherwise
     */
    private boolean isReadyToSend(EmailCampaign campaign) {
        if (campaign.getTemplate() == null) {
            log.error("Campaign {} has no template configured", campaign.getId());
            return false;
        }

        if (campaign.getProvider() == null) {
            log.error("Campaign {} has no email provider configured", campaign.getId());
            return false;
        }

        if (campaign.getContactList() == null && campaign.getSegment() == null) {
            log.error("Campaign {} has no contact list or segment configured", campaign.getId());
            return false;
        }

        if (campaign.getFromEmail() == null || campaign.getFromEmail().isEmpty()) {
            log.error("Campaign {} has no from email configured", campaign.getId());
            return false;
        }

        if (campaign.getSubjectLine() == null || campaign.getSubjectLine().isEmpty()) {
            log.error("Campaign {} has no subject line configured", campaign.getId());
            return false;
        }

        return true;
    }

    /**
     * Manually trigger the scheduled campaign processor (for testing).
     */
    public void triggerScheduledCampaignProcessing() {
        log.info("Manually triggering scheduled campaign processing");
        processScheduledCampaigns();
    }
}
