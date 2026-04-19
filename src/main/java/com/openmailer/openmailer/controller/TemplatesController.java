package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.model.EmailTemplate;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.template.EmailTemplateService;
import com.openmailer.openmailer.service.template.TemplateRendererService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/templates")
public class TemplatesController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final EmailTemplateService templateService;
    private final TemplateRendererService rendererService;

    public TemplatesController(EmailTemplateService templateService, TemplateRendererService rendererService) {
        this.templateService = templateService;
        this.rendererService = rendererService;
    }

    @GetMapping
    public String list(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) String search,
        Model model
    ) {
        String userId = userDetails.getUser().getId();
        List<TemplateListItemView> templates = templateService.findByUserId(userId).stream()
            .filter(template -> matchesSearch(template, search))
            .map(this::toListItemView)
            .sorted((left, right) -> right.updatedAtRaw().compareTo(left.updatedAtRaw()))
            .toList();

        model.addAttribute("pageTitle", "Templates - OpenMailer");
        model.addAttribute("templates", templates);
        model.addAttribute("search", search);
        model.addAttribute("totalTemplates", templateService.countByUserId(userId));
        model.addAttribute("activeTemplates", templates.stream().filter(TemplateListItemView::active).count());
        model.addAttribute("templatesWithVariables", templates.stream().filter(template -> template.variableCount() > 0).count());
        return "templates/list";
    }

    @GetMapping("/new")
    public String add(Model model) {
        model.addAttribute("pageTitle", "New Template - OpenMailer");
        model.addAttribute("templateForm", new TemplateForm());
        model.addAttribute("mode", "create");
        model.addAttribute("availableVariables", defaultVariables());
        return "templates/form";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("templateForm") TemplateForm templateForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "New Template - OpenMailer");
            model.addAttribute("mode", "create");
            model.addAttribute("availableVariables", defaultVariables());
            return "templates/form";
        }

        EmailTemplate template = toEntity(templateForm, userDetails);
        EmailTemplate saved = templateService.createTemplate(template);
        redirectAttributes.addFlashAttribute("successMessage", "Template created successfully.");
        return "redirect:/templates/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String view(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        EmailTemplate template = templateService.findByIdAndUserId(id, userDetails.getUser().getId());
        TemplateDetailView detail = toDetailView(template);
        Map<String, String> sampleData = sampleData();

        model.addAttribute("pageTitle", template.getName() + " - Templates - OpenMailer");
        model.addAttribute("template", detail);
        model.addAttribute("renderedSubject", rendererService.render(template.getSubject(), sampleData));
        model.addAttribute("renderedPreviewText", rendererService.render(template.getPreviewText(), sampleData));
        model.addAttribute("renderedHtml", rendererService.render(template.getHtmlContent(), sampleData));
        model.addAttribute("renderedText", rendererService.render(template.getPlainTextContent(), sampleData));
        model.addAttribute("sampleData", sampleData.entrySet());
        model.addAttribute("variables", detail.variables());
        return "templates/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        EmailTemplate template = templateService.findByIdAndUserId(id, userDetails.getUser().getId());
        model.addAttribute("pageTitle", "Edit Template - OpenMailer");
        model.addAttribute("templateForm", toForm(template));
        model.addAttribute("mode", "edit");
        model.addAttribute("templateId", id);
        model.addAttribute("availableVariables", defaultVariables());
        return "templates/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("templateForm") TemplateForm templateForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Template - OpenMailer");
            model.addAttribute("mode", "edit");
            model.addAttribute("templateId", id);
            model.addAttribute("availableVariables", defaultVariables());
            return "templates/form";
        }

        EmailTemplate updated = toEntity(templateForm, userDetails);
        templateService.updateTemplate(id, userDetails.getUser().getId(), updated);
        redirectAttributes.addFlashAttribute("successMessage", "Template updated successfully.");
        return "redirect:/templates/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        templateService.deleteTemplate(id, userDetails.getUser().getId());
        redirectAttributes.addFlashAttribute("successMessage", "Template deleted successfully.");
        return "redirect:/templates";
    }

    private boolean matchesSearch(EmailTemplate template, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.trim().toLowerCase();
        return contains(template.getName(), term)
            || contains(template.getSubject(), term)
            || contains(template.getDescription(), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private EmailTemplate toEntity(TemplateForm form, CustomUserDetails userDetails) {
        EmailTemplate template = new EmailTemplate();
        template.setName(form.getName().trim());
        template.setSubject(form.getSubject().trim());
        template.setDescription(blankToNull(form.getDescription()));
        template.setPreviewText(blankToNull(form.getPreviewText()));
        template.setHtmlContent(form.getHtmlContent());
        template.setPlainTextContent(blankToNull(form.getPlainTextContent()));
        template.setBody(form.getHtmlContent());
        template.setCreatedBy(userDetails.getUser());
        template.setUserId(userDetails.getUser().getId());
        template.setIsHtml(true);
        template.setIsActive(true);
        template.setTemplateType("CUSTOM");
        return template;
    }

    private TemplateForm toForm(EmailTemplate template) {
        TemplateForm form = new TemplateForm();
        form.setName(template.getName());
        form.setSubject(template.getSubject());
        form.setDescription(template.getDescription());
        form.setPreviewText(template.getPreviewText());
        form.setHtmlContent(template.getHtmlContent());
        form.setPlainTextContent(template.getPlainTextContent());
        return form;
    }

    private TemplateListItemView toListItemView(EmailTemplate template) {
        Set<String> variables = extractAllVariables(template);
        return new TemplateListItemView(
            template.getId(),
            template.getName(),
            template.getSubject(),
            template.getDescription(),
            Boolean.TRUE.equals(template.getIsActive()),
            variables.size(),
            snippet(template.getPreviewText() != null ? template.getPreviewText() : template.getPlainTextContent()),
            formatDateTime(template.getUpdatedAt()),
            template.getUpdatedAt()
        );
    }

    private TemplateDetailView toDetailView(EmailTemplate template) {
        Set<String> variables = extractAllVariables(template);
        return new TemplateDetailView(
            template.getId(),
            template.getName(),
            template.getSubject(),
            template.getDescription(),
            template.getPreviewText(),
            Boolean.TRUE.equals(template.getIsActive()),
            variables,
            formatDateTime(template.getCreatedAt()),
            formatDateTime(template.getUpdatedAt())
        );
    }

    private Set<String> extractAllVariables(EmailTemplate template) {
        java.util.Set<String> variables = new java.util.TreeSet<>();
        variables.addAll(rendererService.extractVariables(template.getSubject()));
        variables.addAll(rendererService.extractVariables(template.getPreviewText()));
        variables.addAll(rendererService.extractVariables(template.getHtmlContent()));
        variables.addAll(rendererService.extractVariables(template.getPlainTextContent()));
        return variables;
    }

    private List<String> defaultVariables() {
        return List.of(
            "first_name",
            "last_name",
            "full_name",
            "email",
            "custom.company",
            "custom.jobTitle",
            "current_year"
        );
    }

    private Map<String, String> sampleData() {
        return Map.of(
            "first_name", "Alex",
            "last_name", "Morgan",
            "full_name", "Alex Morgan",
            "email", "alex@example.com",
            "custom.company", "Northwind Studio",
            "custom.jobTitle", "Growth Lead",
            "current_year", String.valueOf(Year.now().getValue())
        );
    }

    private String snippet(String value) {
        if (value == null || value.isBlank()) {
            return "No preview text yet.";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 110 ? compact.substring(0, 107) + "..." : compact;
    }

    private String blankToNull(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMAT) : "Never";
    }

    public static class TemplateForm {
        @NotBlank(message = "Template name is required.")
        private String name;

        @NotBlank(message = "Subject is required.")
        private String subject;

        private String description;
        private String previewText;

        @NotBlank(message = "HTML content is required.")
        private String htmlContent;

        private String plainTextContent;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPreviewText() {
            return previewText;
        }

        public void setPreviewText(String previewText) {
            this.previewText = previewText;
        }

        public String getHtmlContent() {
            return htmlContent;
        }

        public void setHtmlContent(String htmlContent) {
            this.htmlContent = htmlContent;
        }

        public String getPlainTextContent() {
            return plainTextContent;
        }

        public void setPlainTextContent(String plainTextContent) {
            this.plainTextContent = plainTextContent;
        }
    }

    public record TemplateListItemView(
        String id,
        String name,
        String subject,
        String description,
        boolean active,
        int variableCount,
        String previewSnippet,
        String updatedAt,
        LocalDateTime updatedAtRaw
    ) {
    }

    public record TemplateDetailView(
        String id,
        String name,
        String subject,
        String description,
        String previewText,
        boolean active,
        Set<String> variables,
        String createdAt,
        String updatedAt
    ) {
    }
}
