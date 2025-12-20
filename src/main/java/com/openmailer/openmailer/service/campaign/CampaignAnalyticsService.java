package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.model.CampaignClick;
import com.openmailer.openmailer.model.CampaignRecipient;
import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.repository.CampaignClickRepository;
import com.openmailer.openmailer.repository.CampaignRecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for campaign analytics and reporting.
 * Aggregates statistics, generates reports, and provides insights.
 */
@Service
@Transactional(readOnly = true)
public class CampaignAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(CampaignAnalyticsService.class);

    private final CampaignService campaignService;
    private final CampaignRecipientRepository recipientRepository;
    private final CampaignClickRepository clickRepository;

    @Autowired
    public CampaignAnalyticsService(
            CampaignService campaignService,
            CampaignRecipientRepository recipientRepository,
            CampaignClickRepository clickRepository) {
        this.campaignService = campaignService;
        this.recipientRepository = recipientRepository;
        this.clickRepository = clickRepository;
    }

    /**
     * Gets comprehensive analytics for a campaign.
     *
     * @param campaignId the campaign ID
     * @return campaign analytics
     */
    public CampaignAnalytics getCampaignAnalytics(String campaignId) {
        log.info("Generating analytics for campaign: {}", campaignId);

        EmailCampaign campaign = campaignService.findById(campaignId);
        CampaignAnalytics analytics = new CampaignAnalytics();

        analytics.setCampaignId(campaignId);
        analytics.setCampaignName(campaign.getName());
        analytics.setStatus(campaign.getStatus());
        analytics.setSentAt(campaign.getSentAt());

        // Get all recipients
        List<CampaignRecipient> recipients = recipientRepository.findByCampaignId(campaignId);

        // Calculate basic stats
        long totalRecipients = recipients.size();
        long sentCount = recipients.stream().filter(r -> "SENT".equals(r.getStatus())).count();
        long deliveredCount = recipients.stream().filter(r -> r.getDeliveredAt() != null).count();
        long openedCount = recipients.stream().filter(r -> r.getOpenedAt() != null).count();
        long clickedCount = recipients.stream().filter(r -> r.getClickedAt() != null).count();
        long bouncedCount = recipients.stream().filter(r -> "BOUNCED".equals(r.getStatus())).count();
        long complainedCount = recipients.stream().filter(r -> r.getComplainedAt() != null).count();

        analytics.setTotalRecipients(totalRecipients);
        analytics.setSentCount(sentCount);
        analytics.setDeliveredCount(deliveredCount);
        analytics.setOpenedCount(openedCount);
        analytics.setClickedCount(clickedCount);
        analytics.setBouncedCount(bouncedCount);
        analytics.setComplainedCount(complainedCount);

        // Calculate rates
        if (totalRecipients > 0) {
            analytics.setDeliveryRate(calculateRate(deliveredCount, totalRecipients));
            analytics.setOpenRate(calculateRate(openedCount, deliveredCount > 0 ? deliveredCount : totalRecipients));
            analytics.setClickRate(calculateRate(clickedCount, deliveredCount > 0 ? deliveredCount : totalRecipients));
            analytics.setBounceRate(calculateRate(bouncedCount, totalRecipients));
            analytics.setComplaintRate(calculateRate(complainedCount, totalRecipients));
        }

        // Calculate click-to-open rate (CTOR)
        if (openedCount > 0) {
            analytics.setClickToOpenRate(calculateRate(clickedCount, openedCount));
        }

        // Get top links
        analytics.setTopLinks(getTopLinks(campaignId, 10));

        // Get engagement timeline
        analytics.setEngagementTimeline(getEngagementTimeline(campaignId));

        log.info("Analytics generated for campaign {}: {} recipients, {}% open rate",
                campaignId, totalRecipients, analytics.getOpenRate());

        return analytics;
    }

    /**
     * Gets analytics for multiple campaigns (dashboard overview).
     *
     * @param userId the user ID
     * @return dashboard analytics
     */
    public DashboardAnalytics getDashboardAnalytics(String userId) {
        log.info("Generating dashboard analytics for user: {}", userId);

        DashboardAnalytics analytics = new DashboardAnalytics();

        List<EmailCampaign> campaigns = campaignService.findByUserId(userId);

        long totalCampaigns = campaigns.size();
        long completedCampaigns = campaigns.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();
        long activeCampaigns = campaigns.stream().filter(c -> "SENDING".equals(c.getStatus())).count();

        analytics.setTotalCampaigns(totalCampaigns);
        analytics.setCompletedCampaigns(completedCampaigns);
        analytics.setActiveCampaigns(activeCampaigns);

        // Aggregate stats across all campaigns
        long totalRecipients = 0;
        long totalOpened = 0;
        long totalClicked = 0;
        long totalBounced = 0;

        for (EmailCampaign campaign : campaigns) {
            if (campaign.getTotalRecipients() != null) {
                totalRecipients += campaign.getTotalRecipients();
            }
            if (campaign.getSentCount() != null && campaign.getOpenRate() != null) {
                totalOpened += (long) (campaign.getSentCount() * campaign.getOpenRate().doubleValue() / 100);
            }
            if (campaign.getSentCount() != null && campaign.getClickRate() != null) {
                totalClicked += (long) (campaign.getSentCount() * campaign.getClickRate().doubleValue() / 100);
            }
            if (campaign.getBounceRate() != null) {
                totalBounced += (long) (campaign.getTotalRecipients() * campaign.getBounceRate().doubleValue() / 100);
            }
        }

        analytics.setTotalRecipients(totalRecipients);
        analytics.setTotalOpened(totalOpened);
        analytics.setTotalClicked(totalClicked);
        analytics.setTotalBounced(totalBounced);

        // Calculate average rates
        if (totalRecipients > 0) {
            analytics.setAverageOpenRate(calculateRate(totalOpened, totalRecipients));
            analytics.setAverageClickRate(calculateRate(totalClicked, totalRecipients));
            analytics.setAverageBounceRate(calculateRate(totalBounced, totalRecipients));
        }

        // Get recent campaigns
        analytics.setRecentCampaigns(campaigns.stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.getSentAt() != null ? a.getSentAt() : a.getCreatedAt();
                    LocalDateTime bTime = b.getSentAt() != null ? b.getSentAt() : b.getCreatedAt();
                    return bTime.compareTo(aTime);
                })
                .limit(5)
                .map(c -> {
                    CampaignSummary summary = new CampaignSummary();
                    summary.setId(c.getId());
                    summary.setName(c.getName());
                    summary.setStatus(c.getStatus());
                    summary.setSentAt(c.getSentAt());
                    summary.setTotalRecipients(c.getTotalRecipients() != null ? c.getTotalRecipients() : 0);
                    summary.setOpenRate(c.getOpenRate() != null ? c.getOpenRate().doubleValue() : 0.0);
                    return summary;
                })
                .toList());

        return analytics;
    }

    /**
     * Gets engagement timeline for a campaign (opens and clicks over time).
     */
    private List<TimelinePoint> getEngagementTimeline(String campaignId) {
        List<CampaignRecipient> recipients = recipientRepository.findByCampaignId(campaignId);

        // Group by date
        Map<LocalDate, TimelinePoint> timeline = new TreeMap<>();

        for (CampaignRecipient recipient : recipients) {
            if (recipient.getOpenedAt() != null) {
                LocalDate date = recipient.getOpenedAt().toLocalDate();
                TimelinePoint point = timeline.computeIfAbsent(date, d -> new TimelinePoint(d));
                point.setOpens(point.getOpens() + 1);
            }

            if (recipient.getClickedAt() != null) {
                LocalDate date = recipient.getClickedAt().toLocalDate();
                TimelinePoint point = timeline.computeIfAbsent(date, d -> new TimelinePoint(d));
                point.setClicks(point.getClicks() + 1);
            }
        }

        return new ArrayList<>(timeline.values());
    }

    /**
     * Gets top clicked links for a campaign.
     */
    private List<LinkStats> getTopLinks(String campaignId, int limit) {
        List<CampaignClick> clicks = clickRepository.findByCampaignId(campaignId);

        Map<String, LinkStats> linkStatsMap = new HashMap<>();

        for (CampaignClick click : clicks) {
            String url = click.getLink().getOriginalUrl();
            LinkStats stats = linkStatsMap.computeIfAbsent(url, u -> {
                LinkStats ls = new LinkStats();
                ls.setUrl(u);
                ls.setClicks(0);
                ls.setUniqueClicks(new HashSet<>());
                return ls;
            });

            stats.setClicks(stats.getClicks() + 1);
            stats.getUniqueClicks().add(click.getRecipient().getId());
        }

        return linkStatsMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getClicks(), a.getClicks()))
                .limit(limit)
                .peek(stats -> stats.setUniqueClickCount(stats.getUniqueClicks().size()))
                .toList();
    }

    /**
     * Calculates a percentage rate.
     */
    private double calculateRate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Campaign analytics data class.
     */
    public static class CampaignAnalytics {
        private String campaignId;
        private String campaignName;
        private String status;
        private LocalDateTime sentAt;
        private long totalRecipients;
        private long sentCount;
        private long deliveredCount;
        private long openedCount;
        private long clickedCount;
        private long bouncedCount;
        private long complainedCount;
        private double deliveryRate;
        private double openRate;
        private double clickRate;
        private double bounceRate;
        private double complaintRate;
        private double clickToOpenRate;
        private List<LinkStats> topLinks;
        private List<TimelinePoint> engagementTimeline;

        // Getters and setters
        public String getCampaignId() { return campaignId; }
        public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
        public long getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(long totalRecipients) { this.totalRecipients = totalRecipients; }
        public long getSentCount() { return sentCount; }
        public void setSentCount(long sentCount) { this.sentCount = sentCount; }
        public long getDeliveredCount() { return deliveredCount; }
        public void setDeliveredCount(long deliveredCount) { this.deliveredCount = deliveredCount; }
        public long getOpenedCount() { return openedCount; }
        public void setOpenedCount(long openedCount) { this.openedCount = openedCount; }
        public long getClickedCount() { return clickedCount; }
        public void setClickedCount(long clickedCount) { this.clickedCount = clickedCount; }
        public long getBouncedCount() { return bouncedCount; }
        public void setBouncedCount(long bouncedCount) { this.bouncedCount = bouncedCount; }
        public long getComplainedCount() { return complainedCount; }
        public void setComplainedCount(long complainedCount) { this.complainedCount = complainedCount; }
        public double getDeliveryRate() { return deliveryRate; }
        public void setDeliveryRate(double deliveryRate) { this.deliveryRate = deliveryRate; }
        public double getOpenRate() { return openRate; }
        public void setOpenRate(double openRate) { this.openRate = openRate; }
        public double getClickRate() { return clickRate; }
        public void setClickRate(double clickRate) { this.clickRate = clickRate; }
        public double getBounceRate() { return bounceRate; }
        public void setBounceRate(double bounceRate) { this.bounceRate = bounceRate; }
        public double getComplaintRate() { return complaintRate; }
        public void setComplaintRate(double complaintRate) { this.complaintRate = complaintRate; }
        public double getClickToOpenRate() { return clickToOpenRate; }
        public void setClickToOpenRate(double clickToOpenRate) { this.clickToOpenRate = clickToOpenRate; }
        public List<LinkStats> getTopLinks() { return topLinks; }
        public void setTopLinks(List<LinkStats> topLinks) { this.topLinks = topLinks; }
        public List<TimelinePoint> getEngagementTimeline() { return engagementTimeline; }
        public void setEngagementTimeline(List<TimelinePoint> engagementTimeline) { this.engagementTimeline = engagementTimeline; }
    }

    /**
     * Dashboard analytics data class.
     */
    public static class DashboardAnalytics {
        private long totalCampaigns;
        private long completedCampaigns;
        private long activeCampaigns;
        private long totalRecipients;
        private long totalOpened;
        private long totalClicked;
        private long totalBounced;
        private double averageOpenRate;
        private double averageClickRate;
        private double averageBounceRate;
        private List<CampaignSummary> recentCampaigns;

        // Getters and setters
        public long getTotalCampaigns() { return totalCampaigns; }
        public void setTotalCampaigns(long totalCampaigns) { this.totalCampaigns = totalCampaigns; }
        public long getCompletedCampaigns() { return completedCampaigns; }
        public void setCompletedCampaigns(long completedCampaigns) { this.completedCampaigns = completedCampaigns; }
        public long getActiveCampaigns() { return activeCampaigns; }
        public void setActiveCampaigns(long activeCampaigns) { this.activeCampaigns = activeCampaigns; }
        public long getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(long totalRecipients) { this.totalRecipients = totalRecipients; }
        public long getTotalOpened() { return totalOpened; }
        public void setTotalOpened(long totalOpened) { this.totalOpened = totalOpened; }
        public long getTotalClicked() { return totalClicked; }
        public void setTotalClicked(long totalClicked) { this.totalClicked = totalClicked; }
        public long getTotalBounced() { return totalBounced; }
        public void setTotalBounced(long totalBounced) { this.totalBounced = totalBounced; }
        public double getAverageOpenRate() { return averageOpenRate; }
        public void setAverageOpenRate(double averageOpenRate) { this.averageOpenRate = averageOpenRate; }
        public double getAverageClickRate() { return averageClickRate; }
        public void setAverageClickRate(double averageClickRate) { this.averageClickRate = averageClickRate; }
        public double getAverageBounceRate() { return averageBounceRate; }
        public void setAverageBounceRate(double averageBounceRate) { this.averageBounceRate = averageBounceRate; }
        public List<CampaignSummary> getRecentCampaigns() { return recentCampaigns; }
        public void setRecentCampaigns(List<CampaignSummary> recentCampaigns) { this.recentCampaigns = recentCampaigns; }
    }

    /**
     * Campaign summary for dashboard.
     */
    public static class CampaignSummary {
        private String id;
        private String name;
        private String status;
        private LocalDateTime sentAt;
        private int totalRecipients;
        private double openRate;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
        public int getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(int totalRecipients) { this.totalRecipients = totalRecipients; }
        public double getOpenRate() { return openRate; }
        public void setOpenRate(double openRate) { this.openRate = openRate; }
    }

    /**
     * Link statistics.
     */
    public static class LinkStats {
        private String url;
        private int clicks;
        private Set<String> uniqueClicks;
        private int uniqueClickCount;

        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getClicks() { return clicks; }
        public void setClicks(int clicks) { this.clicks = clicks; }
        public Set<String> getUniqueClicks() { return uniqueClicks; }
        public void setUniqueClicks(Set<String> uniqueClicks) { this.uniqueClicks = uniqueClicks; }
        public int getUniqueClickCount() { return uniqueClickCount; }
        public void setUniqueClickCount(int uniqueClickCount) { this.uniqueClickCount = uniqueClickCount; }
    }

    /**
     * Timeline point for engagement chart.
     */
    public static class TimelinePoint {
        private LocalDate date;
        private int opens;
        private int clicks;

        public TimelinePoint(LocalDate date) {
            this.date = date;
        }

        // Getters and setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public int getOpens() { return opens; }
        public void setOpens(int opens) { this.opens = opens; }
        public int getClicks() { return clicks; }
        public void setClicks(int clicks) { this.clicks = clicks; }
    }
}
