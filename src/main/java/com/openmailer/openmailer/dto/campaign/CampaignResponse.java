package com.openmailer.openmailer.dto.campaign;

import com.openmailer.openmailer.model.EmailCampaign;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for campaign data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private Long id;
    private String name;
    private EmailCampaign.CampaignStatus status;
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

    public static CampaignResponse fromEntity(EmailCampaign campaign) {
        CampaignResponse response = new CampaignResponse();
        response.setId(campaign.getId());
        response.setName(campaign.getName());
        response.setStatus(campaign.getStatus());
        response.setSubjectLine(campaign.getSubjectLine());
        response.setPreviewText(campaign.getPreviewText());
        response.setTotalRecipients(campaign.getTotalRecipients());
        response.setSentCount(campaign.getSentCount());
        response.setDeliveredCount(campaign.getDeliveredCount());
        response.setFailedCount(campaign.getFailedCount());
        response.setOpenedCount(campaign.getOpenedCount());
        response.setClickedCount(campaign.getClickedCount());
        response.setScheduledAt(campaign.getScheduledAt());
        response.setSentAt(campaign.getSentAt());
        response.setCreatedAt(campaign.getCreatedAt());
        return response;
    }
}
