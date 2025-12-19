package com.openmailer.openmailer.dto.template;

import com.openmailer.openmailer.model.EmailTemplate;

import java.time.LocalDateTime;

/**
 * Response DTO for email template data
 */
public class TemplateResponse {
    private String id;
    private String name;
    private String description;
    private String htmlContent;
    private String textContent;
    private String previewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TemplateResponse() {
    }

    public TemplateResponse(String id, String name, String description, String htmlContent, String textContent,
                            String previewText, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.htmlContent = htmlContent;
        this.textContent = textContent;
        this.previewText = previewText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TemplateResponse fromEntity(EmailTemplate template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setHtmlContent(template.getHtmlContent());
        response.setTextContent(template.getPlainTextContent());
        response.setPreviewText(template.getPreviewText());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
