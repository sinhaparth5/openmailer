package com.openmailer.openmailer.dto.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating/updating campaigns
 */
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

    public CampaignRequest() {
    }

    public CampaignRequest(String name, Long templateId, Long listId, Long segmentId, String subjectLine,
                           String previewText, String fromName, String fromEmail, String replyToEmail,
                           Long domainId, Long providerId, Boolean trackOpens, Boolean trackClicks, Integer sendSpeed) {
        this.name = name;
        this.templateId = templateId;
        this.listId = listId;
        this.segmentId = segmentId;
        this.subjectLine = subjectLine;
        this.previewText = previewText;
        this.fromName = fromName;
        this.fromEmail = fromEmail;
        this.replyToEmail = replyToEmail;
        this.domainId = domainId;
        this.providerId = providerId;
        this.trackOpens = trackOpens;
        this.trackClicks = trackClicks;
        this.sendSpeed = sendSpeed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getListId() {
        return listId;
    }

    public void setListId(Long listId) {
        this.listId = listId;
    }

    public Long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Long segmentId) {
        this.segmentId = segmentId;
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

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getReplyToEmail() {
        return replyToEmail;
    }

    public void setReplyToEmail(String replyToEmail) {
        this.replyToEmail = replyToEmail;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public Boolean getTrackOpens() {
        return trackOpens;
    }

    public void setTrackOpens(Boolean trackOpens) {
        this.trackOpens = trackOpens;
    }

    public Boolean getTrackClicks() {
        return trackClicks;
    }

    public void setTrackClicks(Boolean trackClicks) {
        this.trackClicks = trackClicks;
    }

    public Integer getSendSpeed() {
        return sendSpeed;
    }

    public void setSendSpeed(Integer sendSpeed) {
        this.sendSpeed = sendSpeed;
    }
}
