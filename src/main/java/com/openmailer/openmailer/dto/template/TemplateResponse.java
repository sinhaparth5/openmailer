package com.openmailer.openmailer.dto.template;

import com.openmailer.openmailer.model.EmailTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for email template data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {
    private Long id;
    private String name;
    private String description;
    private String htmlContent;
    private String textContent;
    private String previewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TemplateResponse fromEntity(EmailTemplate template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setHtmlContent(template.getHtmlContent());
        response.setTextContent(template.getTextContent());
        response.setPreviewText(template.getPreviewText());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}
