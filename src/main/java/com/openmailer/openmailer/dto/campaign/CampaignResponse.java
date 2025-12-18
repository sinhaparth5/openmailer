package com.openmailer.openmailer.dto.campaign;

import com.openmailer.openmailer.model.EmailCampaign;

import java.time.LocalDateTime;

/**
 * Response DTO for campaign data
 */
public class CampaignResponse {
    private Long id;
    private String name;
    private String status;
    private String subjectLine;
    private String previewText;
    private Integer totalRecipients;
    private Integer sentCount;
    private Integer deliveredCount;
    private Integer failedCount;
    private Integer openedCount;
    private Integer clickedCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public CampaignResponse() {
    }

    public CampaignResponse(Long id, String name, String status, String subjectLine, String previewText,
                            Integer totalRecipients, Integer sentCount, Integer deliveredCount, Integer failedCount,
                            Integer openedCount, Integer clickedCount, LocalDateTime scheduledAt,
                            LocalDateTime sentAt, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.subjectLine = subjectLine;
        this.previewText = previewText;
        this.totalRecipients = totalRecipients;
        this.sentCount = sentCount;
        this.deliveredCount = deliveredCount;
        this.failedCount = failedCount;
        this.openedCount = openedCount;
        this.clickedCount = clickedCount;
        this.scheduledAt = scheduledAt;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }

    public static CampaignResponse fromEntity(EmailCampaign campaign) {
        CampaignResponse response = new CampaignResponse();
        response.setId(campaign.getId());
        response.setName(campaign.getName());
        response.setStatus(campaign.getStatus());
        response.setSubjectLine(campaign.getSubjectLine());
        response.setPreviewText(campaign.getPreviewText());
        response.setTotalRecipients(campaign.getTotalRecipients());
        response.setSentCount(campaign.getSentCount());
        response.setFailedCount(campaign.getFailedCount());
        response.setScheduledAt(campaign.getScheduledAt());
        response.setSentAt(campaign.getSentAt());
        response.setCreatedAt(campaign.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubjectLine() {
        return subjectLine;
    }

    public void setSubjectLine(String subjectLine) {
        this.subjectLine = subjectLine;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }

    public Integer getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(Integer totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(Integer deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Integer getOpenedCount() {
        return openedCount;
    }

    public void setOpenedCount(Integer openedCount) {
        this.openedCount = openedCount;
    }

    public Integer getClickedCount() {
        return clickedCount;
    }

    public void setClickedCount(Integer clickedCount) {
        this.clickedCount = clickedCount;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
