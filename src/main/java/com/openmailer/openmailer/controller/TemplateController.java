package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.PaginatedResponse;
import com.openmailer.openmailer.dto.template.TemplateRequest;
import com.openmailer.openmailer.dto.template.TemplateResponse;
import com.openmailer.openmailer.model.EmailTemplate;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.template.EmailTemplateService;
import com.openmailer.openmailer.service.template.TemplateRendererService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for email template management
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private final EmailTemplateService templateService;
    private final TemplateRendererService rendererService;

    public TemplateController(EmailTemplateService templateService, TemplateRendererService rendererService) {
        this.templateService = templateService;
        this.rendererService = rendererService;
    }

    /**
     * GET /api/v1/templates - List all templates with pagination
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<TemplateResponse>> listTemplates(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<EmailTemplate> templates = templateService.findByUserId(user.getId(), pageable);

        List<TemplateResponse> responses = templates.getContent().stream()
                .map(TemplateResponse::fromEntity)
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(
                page, size, templates.getTotalElements(), templates.getTotalPages()
        );

        return ResponseEntity.ok(new PaginatedResponse<>(responses, pagination));
    }

    /**
     * GET /api/v1/templates/{id} - Get template by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        EmailTemplate template = templateService.findById(id);

        // Check ownership
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this template", null));
        }

        return ResponseEntity.ok(ApiResponse.success(TemplateResponse.fromEntity(template)));
    }

    /**
     * POST /api/v1/templates - Create new template
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TemplateRequest request) {

        EmailTemplate template = new EmailTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setHtmlContent(request.getHtmlContent());
        template.setPlainTextContent(request.getTextContent());
        template.setPreviewText(request.getPreviewText());
        template.setCreatedBy(user);
        template.setUserId(user.getId());

        EmailTemplate saved = templateService.createTemplate(template);

        log.info("Template created: {} by user: {}", saved.getId(), user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TemplateResponse.fromEntity(saved), "Template created successfully"));
    }

    /**
     * PUT /api/v1/templates/{id} - Update template
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody TemplateRequest request) {

        EmailTemplate template = templateService.findById(id);

        // Check ownership
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this template", null));
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setHtmlContent(request.getHtmlContent());
        template.setPlainTextContent(request.getTextContent());
        template.setPreviewText(request.getPreviewText());

        EmailTemplate updated = templateService.updateTemplate(id, user.getId(), template);

        log.info("Template updated: {} by user: {}", updated.getId(), user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(TemplateResponse.fromEntity(updated), "Template updated successfully"));
    }

    /**
     * DELETE /api/v1/templates/{id} - Delete template
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        EmailTemplate template = templateService.findById(id);

        // Check ownership
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this template", null));
        }

        templateService.deleteTemplate(id, user.getId());

        log.info("Template deleted: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted successfully"));
    }

    /**
     * POST /api/v1/templates/{id}/preview - Preview template with sample data
     */
    @PostMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<Map<String, String>>> previewTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> sampleData) {

        EmailTemplate template = templateService.findById(id);

        // Check ownership
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this template", null));
        }

        // Use provided sample data or default values
        if (sampleData == null || sampleData.isEmpty()) {
            sampleData = getDefaultSampleData();
        }

        // Render template
        String renderedHtml = rendererService.render(template.getHtmlContent(), sampleData);
        String renderedText = template.getPlainTextContent() != null
                ? rendererService.render(template.getPlainTextContent(), sampleData)
                : null;

        Map<String, String> preview = new HashMap<>();
        preview.put("htmlContent", renderedHtml);
        preview.put("textContent", renderedText);

        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    /**
     * GET /api/v1/templates/{id}/variables - Extract variables from template
     */
    @GetMapping("/{id}/variables")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTemplateVariables(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        EmailTemplate template = templateService.findById(id);

        // Check ownership
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this template", null));
        }

        // Extract variables from HTML content
        java.util.Set<String> variables = rendererService.extractVariables(template.getHtmlContent());

        Map<String, Object> result = new HashMap<>();
        result.put("variables", variables);
        result.put("count", variables.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Default sample data for template preview
     */
    private Map<String, String> getDefaultSampleData() {
        Map<String, String> data = new HashMap<>();
        data.put("first_name", "John");
        data.put("firstName", "John");
        data.put("last_name", "Doe");
        data.put("lastName", "Doe");
        data.put("full_name", "John Doe");
        data.put("fullName", "John Doe");
        data.put("email", "john.doe@example.com");
        data.put("custom.company", "Acme Corp");
        data.put("custom.jobTitle", "Software Engineer");
        data.put("current_year", String.valueOf(java.time.Year.now().getValue()));
        return data;
    }
}
