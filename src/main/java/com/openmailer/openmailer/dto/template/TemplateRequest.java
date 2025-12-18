package com.openmailer.openmailer.dto.template;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating/updating email templates
 */
public class TemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotBlank(message = "HTML content is required")
    private String htmlContent;

    private String textContent;
    private String previewText;

    public TemplateRequest() {
    }

    public TemplateRequest(String name, String description, String htmlContent, String textContent, String previewText) {
        this.name = name;
        this.description = description;
        this.htmlContent = htmlContent;
        this.textContent = textContent;
        this.previewText = previewText;
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
}
