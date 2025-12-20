package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.model.*;
import com.openmailer.openmailer.repository.ContactRepository;
import com.openmailer.openmailer.service.contact.ContactListMembershipService;
import com.openmailer.openmailer.service.contact.SegmentService;
import com.openmailer.openmailer.service.email.EmailSender;
import com.openmailer.openmailer.service.email.EmailSender.EmailSendRequest;
import com.openmailer.openmailer.service.email.EmailSender.EmailSendResponse;
import com.openmailer.openmailer.service.email.provider.ProviderFactory;
import com.openmailer.openmailer.service.template.TemplateRendererService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for asynchronously sending email campaigns.
 * Handles the process of sending emails to all recipients in a campaign.
 */
@Service
public class CampaignSendingService {

    private static final Logger log = LoggerFactory.getLogger(CampaignSendingService.class);

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final CampaignService campaignService;
    private final CampaignRecipientService recipientService;
    private final CampaignLinkService linkService;
    private final TrackingService trackingService;
    private final ContactRepository contactRepository;
    private final ContactListMembershipService membershipService;
    private final SegmentService segmentService;
    private final TemplateRendererService templateRenderer;
    private final ProviderFactory providerFactory;

    @Autowired
    public CampaignSendingService(
            CampaignService campaignService,
            CampaignRecipientService recipientService,
            CampaignLinkService linkService,
            TrackingService trackingService,
            ContactRepository contactRepository,
            ContactListMembershipService membershipService,
            SegmentService segmentService,
            TemplateRendererService templateRenderer,
            ProviderFactory providerFactory) {
        this.campaignService = campaignService;
        this.recipientService = recipientService;
        this.linkService = linkService;
        this.trackingService = trackingService;
        this.contactRepository = contactRepository;
        this.membershipService = membershipService;
        this.segmentService = segmentService;
        this.templateRenderer = templateRenderer;
        this.providerFactory = providerFactory;
    }

    /**
     * Asynchronously sends a campaign to all recipients.
     * This method runs in a separate thread to avoid blocking the controller.
     *
     * @param campaignId the ID (String) of the campaign to send
     */
    @Async
    public void sendCampaignAsync(String campaignId) {
        log.info("Starting async campaign send for campaign ID: {}", campaignId);

        try {
            EmailCampaign campaign = campaignService.findById(campaignId);

            // Validate campaign
            if (campaign.getProvider() == null) {
                throw new IllegalStateException("Campaign has no email provider configured");
            }
            if (campaign.getTemplate() == null) {
                throw new IllegalStateException("Campaign has no template configured");
            }

            // Update campaign status to SENDING
            campaign.setStatus("SENDING");
            campaignService.updateCampaign(campaignId, campaign.getUserId(), campaign);

            // Get all contacts for this campaign
            List<Contact> contacts = getContactsForCampaign(campaign);

            if (contacts.isEmpty()) {
                log.warn("No contacts found for campaign {}", campaignId);
                campaign.setStatus("COMPLETED");
                campaign.setTotalRecipients(0);
                campaign.setSentAt(LocalDateTime.now());
                campaignService.updateCampaign(campaignId, campaign.getUserId(), campaign);
                return;
            }

            log.info("Found {} contacts for campaign {}", contacts.size(), campaignId);

            // Update total recipients
            campaign.setTotalRecipients(contacts.size());
            campaignService.updateCampaign(campaignId, campaign.getUserId(), campaign);

            // Create recipient records
            List<CampaignRecipient> recipients = createRecipientRecords(campaign, contacts);

            // Create email sender
            EmailSender emailSender = providerFactory.createProvider(campaign.getProvider());

            // Send emails with rate limiting
            sendEmailsToRecipients(campaign, recipients, emailSender);

            // Update campaign statistics
            updateCampaignStatistics(campaign);

            // Mark campaign as completed
            campaign.setStatus("COMPLETED");
            campaign.setSentAt(LocalDateTime.now());
            campaignService.updateCampaign(campaignId, campaign.getUserId(), campaign);

            log.info("Campaign {} completed. Sent: {}, Failed: {}",
                    campaignId, campaign.getSentCount(), campaign.getFailedCount());

        } catch (Exception e) {
            log.error("Failed to send campaign {}: {}", campaignId, e.getMessage(), e);

            // Update campaign status to failed
            try {
                EmailCampaign campaign = campaignService.findById(campaignId);
                campaign.setStatus("FAILED");
                campaignService.updateCampaign(campaignId, campaign.getUserId(), campaign);
            } catch (Exception updateError) {
                log.error("Failed to update campaign status: {}", updateError.getMessage());
            }
        }
    }

    /**
     * Gets all contacts for a campaign based on contact list or segment.
     *
     * @param campaign the campaign
     * @return list of contacts
     */
    private List<Contact> getContactsForCampaign(EmailCampaign campaign) {
        if (campaign.getSegment() != null) {
            // TODO: Implement segment contact retrieval when SegmentController is implemented
            log.warn("Segment-based campaigns not fully implemented yet");
            return new ArrayList<>();
        } else if (campaign.getContactList() != null) {
            // Get contacts from the list
            List<String> contactIds = membershipService.getContactIdsByList(campaign.getContactList().getId());
            if (contactIds.isEmpty()) {
                return new ArrayList<>();
            }
            // Get all contacts and filter for SUBSCRIBED status
            return contactRepository.findAllById(contactIds).stream()
                    .filter(contact -> "SUBSCRIBED".equals(contact.getStatus()))
                    .toList();
        } else {
            log.warn("Campaign has no contact list or segment configured");
            return new ArrayList<>();
        }
    }

    /**
     * Creates recipient records for all contacts in the campaign.
     *
     * @param campaign the campaign
     * @param contacts the contacts
     * @return list of recipients
     */
    private List<CampaignRecipient> createRecipientRecords(EmailCampaign campaign, List<Contact> contacts) {
        List<CampaignRecipient> recipients = new ArrayList<>();

        for (Contact contact : contacts) {
            try {
                CampaignRecipient recipient = new CampaignRecipient();
                recipient.setCampaign(campaign);
                recipient.setContact(contact);
                recipient.setStatus("PENDING");
                recipient.setTrackingId(trackingService.generateTrackingId());

                recipients.add(recipientService.createRecipient(recipient));
            } catch (Exception e) {
                log.error("Failed to create recipient for contact {}: {}",
                        contact.getEmail(), e.getMessage());
            }
        }

        return recipients;
    }

    /**
     * Sends emails to all recipients with rate limiting.
     *
     * @param campaign the campaign
     * @param recipients the recipients
     * @param emailSender the email sender
     */
    private void sendEmailsToRecipients(EmailCampaign campaign, List<CampaignRecipient> recipients, EmailSender emailSender) {
        int sentCount = 0;
        int failedCount = 0;
        int sendSpeed = campaign.getSendSpeed() != null ? campaign.getSendSpeed() : 100;

        for (CampaignRecipient recipient : recipients) {
            try {
                // Send the email
                boolean success = sendEmailToRecipient(campaign, recipient, emailSender);

                if (success) {
                    sentCount++;
                } else {
                    failedCount++;
                }

                // Rate limiting: sleep between emails based on send speed
                // sendSpeed is emails per minute, so sleep = 60000 / sendSpeed milliseconds
                if (sendSpeed > 0) {
                    long sleepMs = 60000 / sendSpeed;
                    Thread.sleep(sleepMs);
                }

            } catch (InterruptedException e) {
                log.error("Campaign sending interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error sending email to {}: {}", recipient.getContact().getEmail(), e.getMessage());
                failedCount++;
            }
        }

        // Update campaign counts
        campaign.setSentCount(sentCount);
        campaign.setFailedCount(failedCount);
        campaignService.updateCampaign(campaign.getId(), campaign.getUserId(), campaign);
    }

    /**
     * Sends an email to a single recipient.
     *
     * @param campaign the campaign
     * @param recipient the recipient
     * @param emailSender the email sender
     * @return true if successful, false otherwise
     */
    private boolean sendEmailToRecipient(EmailCampaign campaign, CampaignRecipient recipient, EmailSender emailSender) {
        try {
            Contact contact = recipient.getContact();

            // Render template for this contact
            String htmlBody = templateRenderer.render(campaign.getTemplate().getHtmlContent(), contact);
            String textBody = campaign.getTemplate().getPlainTextContent() != null
                    ? templateRenderer.render(campaign.getTemplate().getPlainTextContent(), contact)
                    : null;
            String subject = templateRenderer.renderSubject(campaign.getSubjectLine(), contact);

            // Add tracking pixel if tracking opens is enabled
            if (Boolean.TRUE.equals(campaign.getTrackOpens())) {
                htmlBody = templateRenderer.addTrackingPixel(htmlBody, recipient.getTrackingId(), baseUrl);
            }

            // Replace links with tracking links if tracking clicks is enabled
            if (Boolean.TRUE.equals(campaign.getTrackClicks())) {
                htmlBody = replaceLinksWithTracking(htmlBody, campaign.getId(), recipient.getTrackingId());
            }

            // Build email request
            EmailSendRequest request = new EmailSendRequest();
            request.setTo(contact.getEmail());
            request.setFrom(campaign.getFromEmail());
            request.setFromName(campaign.getFromName());
            request.setReplyTo(campaign.getReplyToEmail());
            request.setSubject(subject);
            request.setHtmlBody(htmlBody);
            request.setTextBody(textBody);
            request.setTrackingId(recipient.getTrackingId());
            request.setTrackOpens(Boolean.TRUE.equals(campaign.getTrackOpens()));
            request.setTrackClicks(Boolean.TRUE.equals(campaign.getTrackClicks()));

            // Send the email
            EmailSendResponse response = emailSender.send(request);

            if (response.isSuccess()) {
                recipientService.markAsSent(recipient.getId());
                log.info("Email sent to {}: {}", contact.getEmail(), response.getMessageId());
                return true;
            } else {
                recipientService.markAsFailed(recipient.getId(), response.getErrorMessage());
                log.error("Failed to send email to {}: {}", contact.getEmail(), response.getErrorMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending email to recipient {}: {}", recipient.getId(), e.getMessage(), e);
            recipientService.markAsFailed(recipient.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Replaces all links in HTML with tracking links.
     *
     * @param htmlBody the HTML body
     * @param campaignId the campaign ID
     * @param trackingId the recipient tracking ID
     * @return HTML with tracking links
     */
    private String replaceLinksWithTracking(String htmlBody, String campaignId, String trackingId) {
        // Find all links in the HTML
        Pattern linkPattern = Pattern.compile("href=[\"']([^\"']+)[\"']");
        Matcher matcher = linkPattern.matcher(htmlBody);

        Map<String, String> linkMap = new HashMap<>();

        while (matcher.find()) {
            String url = matcher.group(1);
            // Skip mailto, tel, and anchor links
            if (!url.startsWith("mailto:") && !url.startsWith("tel:") && !url.startsWith("#")) {
                // Create or get tracking link
                CampaignLink link = linkService.findOrCreateLink(campaignId, url);
                // Add tracking ID as query parameter to associate click with recipient
                String trackingUrl = baseUrl + "/track/click/" + link.getShortCode() + "?tid=" + trackingId;
                linkMap.put(url, trackingUrl);
            }
        }

        // Replace all original URLs with tracking URLs
        String result = htmlBody;
        for (Map.Entry<String, String> entry : linkMap.entrySet()) {
            result = result.replaceAll("href=[\"']" + Pattern.quote(entry.getKey()) + "[\"']",
                    "href=\"" + entry.getValue() + "\"");
        }

        return result;
    }

    /**
     * Updates campaign statistics based on recipient data.
     *
     * @param campaign the campaign
     */
    private void updateCampaignStatistics(EmailCampaign campaign) {
        long totalRecipients = recipientService.countByCampaign(campaign.getId());
        long sentCount = recipientService.countByStatus(campaign.getId(), "SENT");
        long openedCount = recipientService.countOpened(campaign.getId());
        long clickedCount = recipientService.countClicked(campaign.getId());
        long bouncedCount = recipientService.countBounced(campaign.getId());

        // Calculate rates
        BigDecimal openRate = totalRecipients > 0
                ? BigDecimal.valueOf(openedCount * 100.0 / totalRecipients).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal clickRate = totalRecipients > 0
                ? BigDecimal.valueOf(clickedCount * 100.0 / totalRecipients).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal bounceRate = totalRecipients > 0
                ? BigDecimal.valueOf(bouncedCount * 100.0 / totalRecipients).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        campaign.setOpenRate(openRate);
        campaign.setClickRate(clickRate);
        campaign.setBounceRate(bounceRate);

        campaignService.updateCampaign(campaign.getId(), campaign.getUserId(), campaign);
    }

    /**
     * Sends a campaign synchronously (for testing purposes).
     *
     * @param campaignId the ID (String) of the campaign to send
     */
    public void sendCampaignSync(String campaignId) {
        log.info("Starting synchronous campaign send for campaign ID: {}", campaignId);
        sendCampaignAsync(campaignId);
    }
}
