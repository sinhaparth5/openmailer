package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Domain;
import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.ProviderType;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.domain.DomainService;
import com.openmailer.openmailer.service.email.provider.ProviderFactory;
import com.openmailer.openmailer.service.provider.EmailProviderService;
import com.openmailer.openmailer.service.security.EncryptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/providers")
public class ProvidersController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final EmailProviderService providerService;
    private final DomainService domainService;
    private final EncryptionService encryptionService;
    private final ProviderFactory providerFactory;

    public ProvidersController(
        EmailProviderService providerService,
        DomainService domainService,
        EncryptionService encryptionService,
        ProviderFactory providerFactory
    ) {
        this.providerService = providerService;
        this.domainService = domainService;
        this.encryptionService = encryptionService;
        this.providerFactory = providerFactory;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUser().getId();
        List<ProviderListItemView> providers = providerService.findByUserId(userId).stream()
            .map(this::toListItemView)
            .sorted(Comparator
                .comparing(ProviderListItemView::isDefault, Comparator.reverseOrder())
                .thenComparing(ProviderListItemView::createdAtRaw, Comparator.reverseOrder()))
            .toList();
        List<Domain> verifiedDomains = domainService.findByUserId(userId).stream()
            .filter(domain -> "VERIFIED".equalsIgnoreCase(domain.getStatus()))
            .toList();

        model.addAttribute("pageTitle", "Providers - OpenMailer");
        model.addAttribute("providerForm", new ProviderForm());
        model.addAttribute("providers", providers);
        model.addAttribute("domainOptions", verifiedDomains);
        model.addAttribute("totalProviders", providers.size());
        model.addAttribute("activeProviders", providers.stream().filter(ProviderListItemView::active).count());
        model.addAttribute("defaultProviderCount", providers.stream().filter(ProviderListItemView::isDefault).count());
        return "providers/list";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("providerForm") ProviderForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            repopulate(userDetails, model);
            model.addAttribute("pageTitle", "Providers - OpenMailer");
            return "providers/list";
        }

        try {
            EmailProvider provider = buildProvider(form, userDetails.getUser().getId());
            provider.setUser(userDetails.getUser());
            providerService.createProvider(provider);
            redirectAttributes.addFlashAttribute("successMessage", "Provider created successfully.");
            return "redirect:/providers";
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            repopulate(userDetails, model);
            model.addAttribute("pageTitle", "Providers - OpenMailer");
            return "providers/list";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            EmailProvider provider = providerService.findByIdAndUserId(id, userDetails.getUser().getId());
            providerService.setActiveStatus(id, userDetails.getUser().getId(), !Boolean.TRUE.equals(provider.getIsActive()));
            redirectAttributes.addFlashAttribute("successMessage", "Provider status updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/providers";
    }

    @PostMapping("/{id}/default")
    public String setDefault(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            providerService.setAsDefault(id, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Default provider updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/providers";
    }

    @PostMapping("/{id}/test")
    public String test(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            EmailProvider provider = providerService.findByIdAndUserId(id, userDetails.getUser().getId());
            boolean configured = providerFactory.isProviderValid(provider);
            redirectAttributes.addFlashAttribute(
                configured ? "successMessage" : "errorMessage",
                configured ? "Provider configuration looks valid." : "Provider configuration is invalid."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/providers";
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            providerService.deleteProvider(id, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Provider deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/providers";
    }

    private void repopulate(CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUser().getId();
        List<ProviderListItemView> providers = providerService.findByUserId(userId).stream()
            .map(this::toListItemView)
            .sorted(Comparator
                .comparing(ProviderListItemView::isDefault, Comparator.reverseOrder())
                .thenComparing(ProviderListItemView::createdAtRaw, Comparator.reverseOrder()))
            .toList();
        List<Domain> verifiedDomains = domainService.findByUserId(userId).stream()
            .filter(domain -> "VERIFIED".equalsIgnoreCase(domain.getStatus()))
            .toList();
        model.addAttribute("providers", providers);
        model.addAttribute("domainOptions", verifiedDomains);
        model.addAttribute("totalProviders", providers.size());
        model.addAttribute("activeProviders", providers.stream().filter(ProviderListItemView::active).count());
        model.addAttribute("defaultProviderCount", providers.stream().filter(ProviderListItemView::isDefault).count());
    }

    private EmailProvider buildProvider(ProviderForm form, String userId) {
        ProviderType providerType = parseType(form.getProviderType());
        Map<String, String> configuration = buildConfiguration(form, providerType);

        EmailProvider provider = new EmailProvider();
        provider.setProviderName(form.getName().trim());
        provider.setProviderType(providerType);
        provider.setConfigurationMap(encryptSensitiveConfig(configuration));
        provider.setIsActive(true);
        provider.setIsDefault(form.isDefaultProvider());
        provider.setUserId(userId);
        provider.setEmailsSent(0);
        provider.setEmailsFailed(0);
        provider.setStatus("ACTIVE");

        if (form.getDailyLimit() != null && !form.getDailyLimit().isBlank()) {
            provider.setDailyLimit(Integer.parseInt(form.getDailyLimit().trim()));
        }
        if (form.getMonthlyLimit() != null && !form.getMonthlyLimit().isBlank()) {
            provider.setMonthlyLimit(Integer.parseInt(form.getMonthlyLimit().trim()));
        }

        return provider;
    }

    private ProviderType parseType(String rawType) {
        try {
            return ProviderType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ValidationException("Select a valid provider type.", "providerType");
        }
    }

    private Map<String, String> buildConfiguration(ProviderForm form, ProviderType providerType) {
        Map<String, String> configuration = new HashMap<>();
        switch (providerType) {
            case SMTP -> {
                require(form.getSmtpHost(), "smtpHost", "SMTP host is required.");
                require(form.getSmtpUsername(), "smtpUsername", "SMTP username is required.");
                require(form.getSmtpPassword(), "smtpPassword", "SMTP password is required.");
                configuration.put("host", form.getSmtpHost().trim());
                configuration.put("port", blank(form.getSmtpPort()) ? "587" : form.getSmtpPort().trim());
                configuration.put("username", form.getSmtpUsername().trim());
                configuration.put("password", form.getSmtpPassword().trim());
                configuration.put("encryption", blank(form.getSmtpEncryption()) ? "TLS" : form.getSmtpEncryption().trim().toUpperCase(Locale.ROOT));
            }
            case SENDGRID -> {
                require(form.getSendgridApiKey(), "sendgridApiKey", "SendGrid API key is required.");
                configuration.put("apiKey", form.getSendgridApiKey().trim());
            }
            case AWS_SES -> {
                require(form.getAwsAccessKey(), "awsAccessKey", "AWS access key is required.");
                require(form.getAwsSecretKey(), "awsSecretKey", "AWS secret key is required.");
                configuration.put("accessKey", form.getAwsAccessKey().trim());
                configuration.put("secretKey", form.getAwsSecretKey().trim());
                configuration.put("region", blank(form.getAwsRegion()) ? "us-east-1" : form.getAwsRegion().trim());
            }
        }

        if (!blank(form.getFromEmail())) {
            configuration.put("fromEmail", form.getFromEmail().trim());
        }
        if (!blank(form.getFromName())) {
            configuration.put("fromName", form.getFromName().trim());
        }
        return configuration;
    }

    private Map<String, String> encryptSensitiveConfig(Map<String, String> config) {
        Map<String, String> encrypted = new HashMap<>();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            if (isSensitive(entry.getKey()) && entry.getValue() != null && !entry.getValue().isBlank()) {
                encrypted.put(entry.getKey(), encryptionService.encrypt(entry.getValue()));
            } else {
                encrypted.put(entry.getKey(), entry.getValue());
            }
        }
        return encrypted;
    }

    private boolean isSensitive(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
            || normalized.contains("secret")
            || normalized.contains("token")
            || normalized.contains("apikey")
            || normalized.contains("accesskey");
    }

    private void require(String value, String field, String message) {
        if (blank(value)) {
            throw new ValidationException(message, field);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void bindValidationError(BindingResult bindingResult, ValidationException ex) {
        if (ex.getField() != null && !ex.getField().isBlank()) {
            bindingResult.rejectValue(ex.getField(), ex.getField() + ".invalid", ex.getMessage());
        } else {
            bindingResult.reject("provider.invalid", ex.getMessage());
        }
    }

    private ProviderListItemView toListItemView(EmailProvider provider) {
        return new ProviderListItemView(
            provider.getId(),
            provider.getProviderName(),
            provider.getProviderType().name(),
            Boolean.TRUE.equals(provider.getIsActive()),
            Boolean.TRUE.equals(provider.getIsDefault()),
            provider.getDailyLimit(),
            provider.getMonthlyLimit(),
            provider.getEmailsSent() != null ? provider.getEmailsSent() : 0,
            provider.getEmailsFailed() != null ? provider.getEmailsFailed() : 0,
            provider.getLastUsedAt() != null ? provider.getLastUsedAt().format(DATE_TIME_FORMAT) : "Never",
            provider.getCreatedAt() != null ? provider.getCreatedAt().format(DATE_TIME_FORMAT) : "Just now",
            provider.getCreatedAt() != null ? provider.getCreatedAt() : LocalDateTime.MIN
        );
    }

    public record ProviderListItemView(
        String id,
        String name,
        String type,
        boolean active,
        boolean isDefault,
        Integer dailyLimit,
        Integer monthlyLimit,
        int emailsSent,
        int emailsFailed,
        String lastUsedAt,
        String createdAt,
        LocalDateTime createdAtRaw
    ) { }

    public static class ProviderForm {
        @NotBlank(message = "Provider name is required.")
        private String name;
        @NotBlank(message = "Select a provider type.")
        private String providerType = "SMTP";
        private String smtpHost;
        private String smtpPort = "587";
        private String smtpUsername;
        private String smtpPassword;
        private String smtpEncryption = "TLS";
        private String sendgridApiKey;
        private String awsAccessKey;
        private String awsSecretKey;
        private String awsRegion = "us-east-1";
        private String fromEmail;
        private String fromName;
        private String dailyLimit;
        private String monthlyLimit;
        private boolean defaultProvider;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProviderType() { return providerType; }
        public void setProviderType(String providerType) { this.providerType = providerType; }
        public String getSmtpHost() { return smtpHost; }
        public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
        public String getSmtpPort() { return smtpPort; }
        public void setSmtpPort(String smtpPort) { this.smtpPort = smtpPort; }
        public String getSmtpUsername() { return smtpUsername; }
        public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
        public String getSmtpPassword() { return smtpPassword; }
        public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }
        public String getSmtpEncryption() { return smtpEncryption; }
        public void setSmtpEncryption(String smtpEncryption) { this.smtpEncryption = smtpEncryption; }
        public String getSendgridApiKey() { return sendgridApiKey; }
        public void setSendgridApiKey(String sendgridApiKey) { this.sendgridApiKey = sendgridApiKey; }
        public String getAwsAccessKey() { return awsAccessKey; }
        public void setAwsAccessKey(String awsAccessKey) { this.awsAccessKey = awsAccessKey; }
        public String getAwsSecretKey() { return awsSecretKey; }
        public void setAwsSecretKey(String awsSecretKey) { this.awsSecretKey = awsSecretKey; }
        public String getAwsRegion() { return awsRegion; }
        public void setAwsRegion(String awsRegion) { this.awsRegion = awsRegion; }
        public String getFromEmail() { return fromEmail; }
        public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        public String getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(String dailyLimit) { this.dailyLimit = dailyLimit; }
        public String getMonthlyLimit() { return monthlyLimit; }
        public void setMonthlyLimit(String monthlyLimit) { this.monthlyLimit = monthlyLimit; }
        public boolean isDefaultProvider() { return defaultProvider; }
        public void setDefaultProvider(boolean defaultProvider) { this.defaultProvider = defaultProvider; }
    }
}
