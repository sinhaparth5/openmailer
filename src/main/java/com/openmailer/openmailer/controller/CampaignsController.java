package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.Domain;
import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.EmailTemplate;
import com.openmailer.openmailer.model.Segment;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.ContactListRepository;
import com.openmailer.openmailer.repository.DomainRepository;
import com.openmailer.openmailer.repository.EmailProviderRepository;
import com.openmailer.openmailer.repository.EmailTemplateRepository;
import com.openmailer.openmailer.repository.SegmentRepository;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.campaign.CampaignAudienceService;
import com.openmailer.openmailer.service.campaign.CampaignSendingService;
import com.openmailer.openmailer.service.campaign.CampaignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/campaigns")
public class CampaignsController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DATE_TIME_INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private final CampaignService campaignService;
    private final CampaignAudienceService audienceService;
    private final CampaignSendingService campaignSendingService;
    private final EmailTemplateRepository templateRepository;
    private final ContactListRepository listRepository;
    private final SegmentRepository segmentRepository;
    private final DomainRepository domainRepository;
    private final EmailProviderRepository providerRepository;

    public CampaignsController(
        CampaignService campaignService,
        CampaignAudienceService audienceService,
        CampaignSendingService campaignSendingService,
        EmailTemplateRepository templateRepository,
        ContactListRepository listRepository,
        SegmentRepository segmentRepository,
        DomainRepository domainRepository,
        EmailProviderRepository providerRepository
    ) {
        this.campaignService = campaignService;
        this.audienceService = audienceService;
        this.campaignSendingService = campaignSendingService;
        this.templateRepository = templateRepository;
        this.listRepository = listRepository;
        this.segmentRepository = segmentRepository;
        this.domainRepository = domainRepository;
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public String list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        String userId = userDetails.getUser().getId();
        List<CampaignSummaryView> campaigns = campaignService.findByUserId(userId).stream()
            .map(this::toSummaryView)
            .filter(campaign -> status == null || status.isBlank() || status.equals(campaign.status()))
            .filter(campaign -> matchesSearch(campaign, search))
            .sorted(Comparator.comparing(CampaignSummaryView::sortDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        model.addAttribute("pageTitle", "Campaigns - OpenMailer");
        model.addAttribute("totalCampaigns", campaignService.countByUserId(userId));
        model.addAttribute("draftCampaigns", campaignService.countByStatus(userId, "DRAFT"));
        model.addAttribute("scheduledCampaigns", campaignService.countByStatus(userId, "SCHEDULED"));
        model.addAttribute("sentCampaigns", campaignService.countByStatus(userId, "SENT"));
        model.addAttribute("statusFilters", List.of(
            Map.of("value", "", "label", "All Statuses"),
            Map.of("value", "DRAFT", "label", "Draft"),
            Map.of("value", "SCHEDULED", "label", "Scheduled"),
            Map.of("value", "SENDING", "label", "Sending"),
            Map.of("value", "SENT", "label", "Sent"),
            Map.of("value", "PAUSED", "label", "Paused")
        ));
        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 1);
        model.addAttribute("pages", Collections.singletonList(1));

        return "campaigns/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        populateFormMetadata(model, "Create Campaign - OpenMailer", "create");
        CampaignForm form = new CampaignForm();
        model.addAttribute("campaignForm", form);
        populateAudiencePreview(model, null, form);
        return "campaigns/form";
    }

    @PostMapping
    public String createCampaign(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("campaignForm") CampaignForm campaignForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, "Create Campaign - OpenMailer", "create");
            populateAudiencePreview(model, userDetails.getUser().getId(), campaignForm);
            return "campaigns/form";
        }

        String userId = userDetails.getUser().getId();
        EmailCampaign saved;
        try {
            saved = campaignService.createCampaign(toEntity(campaignForm, userDetails.getUser(), userId));
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            populateFormMetadata(model, "Create Campaign - OpenMailer", "create");
            populateAudiencePreview(model, userId, campaignForm);
            return "campaigns/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Campaign created successfully.");
        return "redirect:/campaigns/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String view(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        EmailCampaign campaign = campaignService.findByIdAndUserId(id, userDetails.getUser().getId());
        CampaignDetailView view = toDetailView(campaign);
        LocalDateTime minimumScheduleTime = LocalDateTime.now().plusMinutes(15).withSecond(0).withNano(0);
        LocalDateTime suggestedScheduleTime = minimumScheduleTime.plusHours(1);

        model.addAttribute("pageTitle", "Campaign Details - OpenMailer");
        model.addAttribute("campaign", view);
        model.addAttribute("totalSent", view.totalSent());
        model.addAttribute("totalDelivered", view.totalDelivered());
        model.addAttribute("totalOpened", view.openDisplay());
        model.addAttribute("totalClicked", view.clickDisplay());
        model.addAttribute("totalBounced", view.totalBounced());
        model.addAttribute("totalUnsubscribed", view.totalUnsubscribed());
        model.addAttribute("deliveryRate", view.deliveryRate());
        model.addAttribute("openRate", view.openRate());
        model.addAttribute("clickRate", view.clickRate());
        model.addAttribute("bounceRate", view.bounceRate());
        model.addAttribute("complaintCount", view.complaintCount());
        model.addAttribute("canSendNow", "DRAFT".equals(view.status()) || "SCHEDULED".equals(view.status()));
        model.addAttribute("canSchedule", "DRAFT".equals(view.status()));
        model.addAttribute("canCancelSchedule", "SCHEDULED".equals(view.status()));
        model.addAttribute("minimumScheduledAt", minimumScheduleTime.format(DATE_TIME_INPUT_FORMAT));
        model.addAttribute("suggestedScheduledAt", suggestedScheduleTime.format(DATE_TIME_INPUT_FORMAT));
        model.addAttribute("actionStateMessage", actionStateMessage(view.status()));
        model.addAttribute("audiencePreflight", audienceService.evaluate(campaign));

        return "campaigns/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        populateFormMetadata(model, "Edit Campaign - OpenMailer", "edit");
        CampaignForm form = toForm(campaignService.findByIdAndUserId(id, userDetails.getUser().getId()));
        model.addAttribute("campaignForm", form);
        populateAudiencePreview(model, userDetails.getUser().getId(), form);
        return "campaigns/form";
    }

    @PostMapping("/{id}/edit")
    public String updateCampaign(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("campaignForm") CampaignForm campaignForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, "Edit Campaign - OpenMailer", "edit");
            populateAudiencePreview(model, userDetails.getUser().getId(), campaignForm);
            return "campaigns/form";
        }

        try {
            campaignService.updateCampaign(id, userDetails.getUser().getId(), toEntity(campaignForm, userDetails.getUser(), userDetails.getUser().getId()));
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            populateFormMetadata(model, "Edit Campaign - OpenMailer", "edit");
            populateAudiencePreview(model, userDetails.getUser().getId(), campaignForm);
            return "campaigns/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Campaign updated successfully.");
        return "redirect:/campaigns/" + id;
    }

    @PostMapping("/{id}/send")
    public String sendCampaign(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            validateAudienceBeforeDelivery(campaignService.findByIdAndUserId(id, userDetails.getUser().getId()));
            campaignService.startSendingCampaign(id, userDetails.getUser().getId());
            campaignSendingService.sendCampaignAsync(id);
            redirectAttributes.addFlashAttribute("successMessage", "Campaign send started successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/campaigns/" + id;
    }

    @PostMapping("/{id}/schedule")
    public String scheduleCampaign(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("scheduledAt") String scheduledAt,
        RedirectAttributes redirectAttributes
    ) {
        try {
            if (scheduledAt == null || scheduledAt.isBlank()) {
                throw new ValidationException("Choose a future date and time for scheduling.", "scheduledAt");
            }
            validateAudienceBeforeDelivery(campaignService.findByIdAndUserId(id, userDetails.getUser().getId()));
            campaignService.scheduleCampaign(id, userDetails.getUser().getId(), LocalDateTime.parse(scheduledAt));
            redirectAttributes.addFlashAttribute("successMessage", "Campaign scheduled successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/campaigns/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelCampaign(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            campaignService.cancelScheduledCampaign(id, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Scheduled send canceled.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/campaigns/" + id;
    }

    @PostMapping("/{id}/duplicate")
    public String duplicateCampaign(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            EmailCampaign duplicate = campaignService.duplicateCampaign(id, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Campaign duplicated as a new draft.");
            return "redirect:/campaigns/" + duplicate.getId() + "/edit";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/campaigns/" + id;
        }
    }

    @ModelAttribute
    public void populateDependencies(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return;
        }
        String userId = userDetails.getUser().getId();
        List<EmailTemplate> templates = templateRepository.findByUserId(userId);
        List<ContactList> lists = listRepository.findByUser_Id(userId);
        List<Segment> segments = segmentRepository.findByUserId(userId);
        List<Domain> domains = domainRepository.findByUserIdAndStatus(userId, "VERIFIED");
        List<EmailProvider> providers = providerRepository.findByUserIdAndIsActive(userId, true);

        model.addAttribute("templateOptions", templates);
        model.addAttribute("listOptions", lists);
        model.addAttribute("segmentOptions", segments);
        model.addAttribute("domainOptions", domains);
        model.addAttribute("providerOptions", providers);
        model.addAttribute("campaignDependenciesReady", !templates.isEmpty() && !lists.isEmpty() && !domains.isEmpty() && !providers.isEmpty());
    }

    private boolean matchesSearch(CampaignSummaryView campaign, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase(Locale.ROOT);
        return campaign.name().toLowerCase(Locale.ROOT).contains(query)
            || campaign.subject().toLowerCase(Locale.ROOT).contains(query);
    }

    private CampaignSummaryView toSummaryView(EmailCampaign campaign) {
        LocalDateTime sortDate = campaign.getSentAt() != null
            ? campaign.getSentAt()
            : (campaign.getScheduledAt() != null ? campaign.getScheduledAt() : campaign.getCreatedAt());
        return new CampaignSummaryView(
            campaign.getId(),
            campaign.getName(),
            defaultText(campaign.getSubject(), "No subject set"),
            campaign.getStatus(),
            safeInt(campaign.getTotalRecipients()),
            formatCountFromRate(campaign.getOpenRate(), campaign.getSentCount()),
            formatCountFromRate(campaign.getClickRate(), campaign.getSentCount()),
            formatRate(campaign.getOpenRate()),
            formatRate(campaign.getClickRate()),
            formatDate(sortDate),
            sortDate
        );
    }

    private CampaignDetailView toDetailView(EmailCampaign campaign) {
        int totalSent = safeInt(campaign.getSentCount());
        int totalBounced = safeInt(campaign.getFailedCount());
        int totalDelivered = Math.max(totalSent - totalBounced, 0);
        int totalOpened = formatCountFromRate(campaign.getOpenRate(), totalSent);
        int totalClicked = formatCountFromRate(campaign.getClickRate(), totalSent);
        return new CampaignDetailView(
            campaign.getId(),
            campaign.getName(),
            campaign.getStatus(),
            defaultText(campaign.getSubject(), "No subject set"),
            defaultText(campaign.getPreviewText(), "No preview text"),
            safeInt(campaign.getTotalRecipients()),
            totalSent,
            totalDelivered,
            totalOpened,
            totalClicked,
            totalBounced,
            safeInt(campaign.getUnsubscribeCount()),
            safeInt(campaign.getComplaintCount()),
            defaultText(campaign.getFromName(), "Not configured"),
            defaultText(campaign.getFromEmail(), "Not configured"),
            defaultText(campaign.getReplyToEmail(), "Not configured"),
            campaign.getTemplate() != null ? campaign.getTemplate().getName() : "No template selected",
            campaign.getProvider() != null ? campaign.getProvider().getProviderName() : "No provider selected",
            campaign.getDomain() != null ? campaign.getDomain().getDomainName() : "No domain selected",
            campaign.getContactList() != null ? campaign.getContactList().getName() : "No list selected",
            campaign.getSegment() != null ? campaign.getSegment().getName() : "No segment selected",
            formatRate(campaign.getOpenRate()),
            formatRate(campaign.getClickRate()),
            formatRate(campaign.getBounceRate()),
            percentage(totalDelivered, totalSent),
            formatDate(campaign.getCreatedAt()),
            formatDate(campaign.getScheduledAt()),
            formatDate(campaign.getSentAt())
        );
    }

    private CampaignForm toForm(EmailCampaign campaign) {
        CampaignForm form = new CampaignForm();
        form.id = campaign.getId();
        form.name = campaign.getName();
        form.templateId = campaign.getTemplate() != null ? campaign.getTemplate().getId() : null;
        form.listId = campaign.getContactList() != null ? campaign.getContactList().getId() : null;
        form.segmentId = campaign.getSegment() != null ? campaign.getSegment().getId() : null;
        form.subjectLine = campaign.getSubject();
        form.previewText = campaign.getPreviewText();
        form.fromName = campaign.getFromName();
        form.fromEmail = campaign.getFromEmail();
        form.replyToEmail = campaign.getReplyToEmail();
        form.domainId = campaign.getDomain() != null ? campaign.getDomain().getId() : null;
        form.providerId = campaign.getProvider() != null ? campaign.getProvider().getId() : null;
        form.trackOpens = Boolean.TRUE.equals(campaign.getTrackOpens());
        form.trackClicks = Boolean.TRUE.equals(campaign.getTrackClicks());
        return form;
    }

    private EmailCampaign toEntity(CampaignForm form, User user, String userId) {
        EmailTemplate template = templateRepository.findByIdAndUserId(form.templateId, userId)
            .orElseThrow(() -> new ValidationException("Selected template is invalid.", "templateId"));
        ContactList list = listRepository.findByIdAndUser_Id(form.listId, userId)
            .orElseThrow(() -> new ValidationException("Selected contact list is invalid.", "listId"));
        Domain domain = domainRepository.findByIdAndUserId(form.domainId, userId)
            .orElseThrow(() -> new ValidationException("Selected domain is invalid.", "domainId"));
        EmailProvider provider = providerRepository.findByIdAndUserId(form.providerId, userId)
            .orElseThrow(() -> new ValidationException("Selected provider is invalid.", "providerId"));
        Segment segment = blank(form.segmentId)
            ? null
            : segmentRepository.findByIdAndUserId(form.segmentId, userId)
                .orElseThrow(() -> new ValidationException("Selected segment is invalid.", "segmentId"));

        EmailCampaign campaign = new EmailCampaign();
        campaign.setName(form.name.trim());
        campaign.setTemplate(template);
        campaign.setContactList(list);
        campaign.setSegment(segment);
        campaign.setSubjectLine(form.subjectLine.trim());
        campaign.setPreviewText(blank(form.previewText) ? null : form.previewText.trim());
        campaign.setFromName(form.fromName.trim());
        campaign.setFromEmail(form.fromEmail.trim());
        campaign.setReplyToEmail(blank(form.replyToEmail) ? null : form.replyToEmail.trim());
        campaign.setDomain(domain);
        campaign.setProvider(provider);
        campaign.setTrackOpens(form.trackOpens);
        campaign.setTrackClicks(form.trackClicks);
        campaign.setStatus("DRAFT");
        campaign.setTotalRecipients(audienceService.evaluate(userId, list.getId(), form.segmentId).getReachableRecipients());
        campaign.setCreatedBy(user);
        campaign.setUserId(userId);
        return campaign;
    }

    private void populateAudiencePreview(Model model, String userId, CampaignForm form) {
        CampaignAudienceService.AudiencePreflight preflight;
        if (userId == null) {
            preflight = CampaignAudienceService.AudiencePreflight.empty(false, null);
        } else {
            try {
                preflight = audienceService.evaluate(userId, form.listId, form.segmentId);
            } catch (RuntimeException ex) {
                preflight = CampaignAudienceService.AudiencePreflight.empty(
                    form.segmentId != null && !form.segmentId.isBlank(),
                    null
                );
            }
        }
        model.addAttribute("audiencePreflight", preflight);
    }

    private void validateAudienceBeforeDelivery(EmailCampaign campaign) {
        CampaignAudienceService.AudiencePreflight preflight = audienceService.evaluate(campaign);
        if (preflight.isZeroAudience()) {
            throw new ValidationException("This campaign has no reachable subscribed recipients. Review the selected list before sending.");
        }
    }

    private void populateFormMetadata(Model model, String pageTitle, String mode) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("mode", mode);
    }

    private String actionStateMessage(String status) {
        return switch (status) {
            case "DRAFT" -> "This draft is ready for a final review, an immediate send, or a scheduled delivery window.";
            case "SCHEDULED" -> "This campaign is queued for later delivery. You can still send it immediately or cancel the scheduled time.";
            case "SENDING" -> "Delivery has started. Configuration is locked while recipients are being processed.";
            case "SENT" -> "This campaign has completed its send flow. Review the metrics and duplicate the setup pattern in a new draft if needed.";
            default -> "This campaign is not in a state where delivery actions are available.";
        };
    }

    private void bindValidationError(BindingResult bindingResult, ValidationException ex) {
        if (ex.getField() != null && !ex.getField().isBlank()) {
            bindingResult.rejectValue(ex.getField(), "validation", ex.getMessage());
            return;
        }
        bindingResult.reject("validation", ex.getMessage());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private int formatCountFromRate(BigDecimal rate, Integer total) {
        if (rate == null || total == null || total == 0) {
            return 0;
        }
        return rate.multiply(BigDecimal.valueOf(total))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .intValue();
    }

    private String formatRate(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
            .toPlainString() + "%";
    }

    private String formatDate(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMAT) : "Not scheduled";
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record CampaignSummaryView(
        String id,
        String name,
        String subject,
        String status,
        int recipients,
        int opens,
        int clicks,
        String openRate,
        String clickRate,
        String sentDate,
        LocalDateTime sortDate
    ) { }

    private record CampaignDetailView(
        String id,
        String name,
        String status,
        String subject,
        String previewText,
        int recipients,
        int totalSent,
        int totalDelivered,
        int openDisplay,
        int clickDisplay,
        int totalBounced,
        int totalUnsubscribed,
        int complaintCount,
        String fromName,
        String fromEmail,
        String replyToEmail,
        String templateName,
        String providerName,
        String domainName,
        String audienceName,
        String segmentName,
        String openRate,
        String clickRate,
        String bounceRate,
        String deliveryRate,
        String createdDate,
        String scheduledDate,
        String sentDate
    ) { }

    public static class CampaignForm {
        private String id;
        @NotBlank(message = "Campaign name is required.")
        private String name;
        @NotBlank(message = "Select a template.")
        private String templateId;
        @NotBlank(message = "Select a contact list.")
        private String listId;
        private String segmentId;
        @NotBlank(message = "Subject line is required.")
        private String subjectLine;
        private String previewText;
        @NotBlank(message = "From name is required.")
        private String fromName;
        @NotBlank(message = "From email is required.")
        @Email(message = "Enter a valid from email address.")
        private String fromEmail;
        private String replyToEmail;
        @NotBlank(message = "Select a verified domain.")
        private String domainId;
        @NotBlank(message = "Select an active provider.")
        private String providerId;
        private boolean trackOpens = true;
        private boolean trackClicks = true;

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

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public String getListId() {
            return listId;
        }

        public void setListId(String listId) {
            this.listId = listId;
        }

        public String getSegmentId() {
            return segmentId;
        }

        public void setSegmentId(String segmentId) {
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

        public String getDomainId() {
            return domainId;
        }

        public void setDomainId(String domainId) {
            this.domainId = domainId;
        }

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public boolean isTrackOpens() {
            return trackOpens;
        }

        public void setTrackOpens(boolean trackOpens) {
            this.trackOpens = trackOpens;
        }

        public boolean isTrackClicks() {
            return trackClicks;
        }

        public void setTrackClicks(boolean trackClicks) {
            this.trackClicks = trackClicks;
        }
    }
}
