package com.openmailer.openmailer.dto.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating campaigns
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequest {

    @NotBlank(message = "Campaign name is required")
    private String name;

    @NotNull(message = "Template ID is required")
    private Long templateId;

    @NotNull(message = "List ID is required")
    private Long listId;

    private Long segmentId;

    @NotBlank(message = "Subject line is required")
    private String subjectLine;

    private String previewText;

    @NotBlank(message = "From name is required")
    private String fromName;

    @NotBlank(message = "From email is required")
    private String fromEmail;

    private String replyToEmail;

    @NotNull(message = "Domain ID is required")
    private Long domainId;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    private Boolean trackOpens = true;
    private Boolean trackClicks = true;
    private Integer sendSpeed = 100; // emails per minute
}
