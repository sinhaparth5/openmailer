package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.PaginatedResponse;
import com.openmailer.openmailer.dto.campaign.CampaignRequest;
import com.openmailer.openmailer.dto.campaign.CampaignResponse;
import com.openmailer.openmailer.model.*;
import com.openmailer.openmailer.service.campaign.CampaignService;
import com.openmailer.openmailer.service.contact.ContactListService;
import com.openmailer.openmailer.service.domain.DomainService;
import com.openmailer.openmailer.service.provider.EmailProviderService;
import com.openmailer.openmailer.service.template.EmailTemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for campaign management
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private static final Logger log = LoggerFactory.getLogger(CampaignController.class);

    private final CampaignService campaignService;
    private final EmailTemplateService templateService;
    private final ContactListService listService;
    private final DomainService domainService;
    private final EmailProviderService providerService;
    private final com.openmailer.openmailer.service.campaign.CampaignSendingService campaignSendingService;

    public CampaignController(CampaignService campaignService, EmailTemplateService templateService,
                              ContactListService listService, DomainService domainService,
                              EmailProviderService providerService,
                              com.openmailer.openmailer.service.campaign.CampaignSendingService campaignSendingService) {
        this.campaignService = campaignService;
        this.templateService = templateService;
        this.listService = listService;
        this.domainService = domainService;
        this.providerService = providerService;
        this.campaignSendingService = campaignSendingService;
    }

    /**
     * GET /api/v1/campaigns - List all campaigns
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<CampaignResponse>> listCampaigns(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size);
        Page<EmailCampaign> campaigns;

        if (status != null) {
            campaigns = campaignService.findByStatus(user.getId(), status, pageable);
        } else {
            campaigns = campaignService.findByUserId(user.getId(), pageable);
        }

        List<CampaignResponse> responses = campaigns.getContent().stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(
                page, size, campaigns.getTotalElements(), campaigns.getTotalPages()
        );

        return ResponseEntity.ok(new PaginatedResponse<>(responses, pagination));
    }

    /**
     * GET /api/v1/campaigns/{id} - Get campaign by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailCampaign campaign = campaignService.findById(id);

        // Check ownership
        if (!campaign.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this campaign", null));
        }

        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.fromEntity(campaign)));
    }

    /**
     * POST /api/v1/campaigns - Create new campaign
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CampaignRequest request) {

        // Validate template
        EmailTemplate template = templateService.findById(request.getTemplateId());
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this template", "templateId"));
        }

        // Validate list
        ContactList list = listService.findById(request.getListId());
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", "listId"));
        }

        // Validate domain
        Domain domain = domainService.findById(request.getDomainId());
        if (!domain.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this domain", "domainId"));
        }

        // Check if domain is verified
        if (!"VERIFIED".equals(domain.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("DOMAIN_NOT_VERIFIED", "Domain must be verified before sending", "domainId"));
        }

        // Validate provider
        EmailProvider provider = providerService.findById(request.getProviderId());
        if (!provider.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this provider", "providerId"));
        }

        // Create campaign
        EmailCampaign campaign = new EmailCampaign();
        campaign.setName(request.getName());
        campaign.setTemplate(template);
        campaign.setContactList(list);
        campaign.setSubjectLine(request.getSubjectLine());
        campaign.setPreviewText(request.getPreviewText());
        campaign.setFromName(request.getFromName());
        campaign.setFromEmail(request.getFromEmail());
        campaign.setReplyToEmail(request.getReplyToEmail());
        campaign.setDomain(domain);
        campaign.setProvider(provider);
        campaign.setTrackOpens(request.getTrackOpens());
        campaign.setTrackClicks(request.getTrackClicks());
        campaign.setSendSpeed(request.getSendSpeed());
        campaign.setStatus("DRAFT");
        campaign.setTotalRecipients(list.getTotalContacts());
        campaign.setCreatedBy(user);
        campaign.setUserId(user.getId());

        EmailCampaign saved = campaignService.createCampaign(campaign);

        log.info("Campaign created: {} by user: {}", saved.getId(), user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CampaignResponse.fromEntity(saved), "Campaign created successfully"));
    }

    /**
     * PUT /api/v1/campaigns/{id} - Update campaign (only if in DRAFT status)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody CampaignRequest request) {

        EmailCampaign campaign = campaignService.findById(id);

        // Check ownership
        if (!campaign.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this campaign", null));
        }

        // Only allow updates if campaign is DRAFT
        if (!"DRAFT".equals(campaign.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "Can only update campaigns in DRAFT status", "status"));
        }

        // Validate and update template
        EmailTemplate template = templateService.findById(request.getTemplateId());
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this template", "templateId"));
        }

        // Validate and update list
        ContactList list = listService.findById(request.getListId());
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", "listId"));
        }

        campaign.setName(request.getName());
        campaign.setTemplate(template);
        campaign.setContactList(list);
        campaign.setSubjectLine(request.getSubjectLine());
        campaign.setPreviewText(request.getPreviewText());
        campaign.setFromName(request.getFromName());
        campaign.setFromEmail(request.getFromEmail());
        campaign.setReplyToEmail(request.getReplyToEmail());
        campaign.setTrackOpens(request.getTrackOpens());
        campaign.setTrackClicks(request.getTrackClicks());
        campaign.setSendSpeed(request.getSendSpeed());
        campaign.setTotalRecipients(list.getTotalContacts());

        EmailCampaign updated = campaignService.updateCampaign(id, user.getId(), campaign);

        log.info("Campaign updated: {} by user: {}", updated.getId(), user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.fromEntity(updated), "Campaign updated successfully"));
    }

    /**
     * DELETE /api/v1/campaigns/{id} - Delete campaign (only if in DRAFT status)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailCampaign campaign = campaignService.findById(id);

        // Check ownership
        if (!campaign.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this campaign", null));
        }

        // Only allow deletion if campaign is DRAFT
        if (!"DRAFT".equals(campaign.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "Can only delete campaigns in DRAFT status", "status"));
        }

        campaignService.deleteCampaign(id, user.getId());

        log.info("Campaign deleted: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(null, "Campaign deleted successfully"));
    }

    /**
     * POST /api/v1/campaigns/{id}/schedule - Schedule campaign for future sending
     */
    @PostMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<CampaignResponse>> scheduleCampaign(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        EmailCampaign campaign = campaignService.findById(id);

        // Check ownership
        if (!campaign.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this campaign", null));
        }

        // Only allow scheduling if campaign is DRAFT
        if (!"DRAFT".equals(campaign.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "Can only schedule campaigns in DRAFT status", "status"));
        }

        String scheduledAtStr = request.get("scheduledAt");
        if (scheduledAtStr == null || scheduledAtStr.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "scheduledAt is required", "scheduledAt"));
        }

        LocalDateTime scheduledAt = LocalDateTime.parse(scheduledAtStr);

        // Validate scheduled time is in the future
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_SCHEDULED_TIME", "Scheduled time must be in the future", "scheduledAt"));
        }

        campaign.setScheduledAt(scheduledAt);
        campaign.setStatus("SCHEDULED");

        EmailCampaign updated = campaignService.updateCampaign(id, user.getId(), campaign);

        log.info("Campaign scheduled: {} for {} by user: {}", id, scheduledAt, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.fromEntity(updated), "Campaign scheduled successfully"));
    }

    /**
     * POST /api/v1/campaigns/{id}/send - Send campaign immediately
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<CampaignResponse>> sendCampaign(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailCampaign campaign = campaignService.findById(id);

        // Check ownership
        if (!campaign.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this campaign", null));
        }

        // Only allow sending if campaign is DRAFT or SCHEDULED
        if (!"DRAFT".equals(campaign.getStatus()) &&
                !"SCHEDULED".equals(campaign.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "Campaign is not in a sendable state", "status"));
        }

        // Update status to SENDING
        campaign.setStatus("SENDING");
        campaign.setSentAt(LocalDateTime.now());
        campaignService.updateCampaign(id, user.getId(), campaign);

        // Trigger async campaign sending service
        campaignSendingService.sendCampaignAsync(id);

        log.info("Campaign send initiated: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(
                CampaignResponse.fromEntity(campaign),
                "Campaign is being sent. Check analytics for progress."));
    }

    /**
     * POST /api/v1/campaigns/{id}/cancel - Cancel scheduled campaign
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CampaignResponse>> cancelCampaign(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailCampaign campaign = campaignService.findById(id);

        // Check ownership
        if (!campaign.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this campaign", null));
        }

        // Only allow cancellation if campaign is SCHEDULED
        if (!"SCHEDULED".equals(campaign.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "Can only cancel scheduled campaigns", "status"));
        }

        campaign.setStatus("DRAFT");
        campaign.setScheduledAt(null);

        EmailCampaign updated = campaignService.updateCampaign(id, user.getId(), campaign);

        log.info("Campaign cancelled: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.fromEntity(updated), "Campaign cancelled successfully"));
    }

    /**
     * GET /api/v1/campaigns/{id}/stats - Get campaign statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampaignStats(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailCampaign campaign = campaignService.findById(id);

        // Check ownership
        if (!campaign.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this campaign", null));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("campaignId", campaign.getId());
        stats.put("campaignName", campaign.getName());
        stats.put("status", campaign.getStatus());
        stats.put("totalRecipients", campaign.getTotalRecipients());
        stats.put("sentCount", campaign.getSentCount());
        stats.put("failedCount", campaign.getFailedCount());
        stats.put("unsubscribeCount", campaign.getUnsubscribeCount());
        stats.put("complaintCount", campaign.getComplaintCount());

        // TODO: Implement email tracking system
        // This requires:
        // 1. Creating EmailEvent table to track delivery, opens, clicks, bounces
        // 2. Adding tracking pixels to emails for opens
        // 3. Rewriting links in emails to track clicks
        // 4. Processing webhook callbacks from email providers for bounces/deliveries
        // 5. Querying EmailEvent table by campaign_id and event_type for these counts
        stats.put("deliveredCount", 0); // Count of successfully delivered emails
        stats.put("openedCount", 0);    // Count of unique email opens
        stats.put("clickedCount", 0);   // Count of unique link clicks
        stats.put("bouncedCount", 0);   // Count of bounced emails

        // Calculate rates
        stats.put("openRate", "0.00");
        stats.put("clickRate", "0.00");
        stats.put("bounceRate", "0.00");

        stats.put("sentAt", campaign.getSentAt());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
