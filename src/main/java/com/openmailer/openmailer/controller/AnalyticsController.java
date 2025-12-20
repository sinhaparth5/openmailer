package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.campaign.CampaignAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for campaign analytics.
 * Provides endpoints for dashboards, reports, and campaign statistics.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final CampaignAnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(CampaignAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Gets dashboard analytics (overall stats for all campaigns).
     *
     * GET /api/v1/analytics/dashboard
     *
     * @param user the authenticated user
     * @return dashboard analytics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<CampaignAnalyticsService.DashboardAnalytics> getDashboard(
            @AuthenticationPrincipal User user) {

        CampaignAnalyticsService.DashboardAnalytics analytics =
                analyticsService.getDashboardAnalytics(user.getId());

        return ResponseEntity.ok(analytics);
    }

    /**
     * Gets detailed analytics for a specific campaign.
     *
     * GET /api/v1/analytics/campaigns/{campaignId}
     *
     * @param campaignId the campaign ID
     * @param user the authenticated user
     * @return campaign analytics
     */
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<CampaignAnalyticsService.CampaignAnalytics> getCampaignAnalytics(
            @PathVariable String campaignId,
            @AuthenticationPrincipal User user) {

        // Note: In production, you should verify the user owns this campaign
        CampaignAnalyticsService.CampaignAnalytics analytics =
                analyticsService.getCampaignAnalytics(campaignId);

        return ResponseEntity.ok(analytics);
    }

    /**
     * Gets engagement timeline for a campaign (opens and clicks over time).
     *
     * GET /api/v1/analytics/campaigns/{campaignId}/timeline
     *
     * @param campaignId the campaign ID
     * @param user the authenticated user
     * @return engagement timeline
     */
    @GetMapping("/campaigns/{campaignId}/timeline")
    public ResponseEntity<?> getCampaignTimeline(
            @PathVariable String campaignId,
            @AuthenticationPrincipal User user) {

        CampaignAnalyticsService.CampaignAnalytics analytics =
                analyticsService.getCampaignAnalytics(campaignId);

        return ResponseEntity.ok(analytics.getEngagementTimeline());
    }

    /**
     * Gets top clicked links for a campaign.
     *
     * GET /api/v1/analytics/campaigns/{campaignId}/links
     *
     * @param campaignId the campaign ID
     * @param user the authenticated user
     * @return top links with click stats
     */
    @GetMapping("/campaigns/{campaignId}/links")
    public ResponseEntity<?> getTopLinks(
            @PathVariable String campaignId,
            @AuthenticationPrincipal User user) {

        CampaignAnalyticsService.CampaignAnalytics analytics =
                analyticsService.getCampaignAnalytics(campaignId);

        return ResponseEntity.ok(analytics.getTopLinks());
    }
}
