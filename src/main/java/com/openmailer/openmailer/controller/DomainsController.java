package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Domain;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.domain.DkimKeyGenerationService;
import com.openmailer.openmailer.service.domain.DnsVerificationService;
import com.openmailer.openmailer.service.domain.DomainService;
import com.openmailer.openmailer.service.security.EncryptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/domains")
public class DomainsController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final DomainService domainService;
    private final DkimKeyGenerationService dkimKeyGenerationService;
    private final EncryptionService encryptionService;
    private final DnsVerificationService dnsVerificationService;

    public DomainsController(
        DomainService domainService,
        DkimKeyGenerationService dkimKeyGenerationService,
        EncryptionService encryptionService,
        DnsVerificationService dnsVerificationService
    ) {
        this.domainService = domainService;
        this.dkimKeyGenerationService = dkimKeyGenerationService;
        this.encryptionService = encryptionService;
        this.dnsVerificationService = dnsVerificationService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUser().getId();
        List<DomainListItemView> domains = domainService.findByUserId(userId).stream()
            .map(this::toListItemView)
            .sorted((left, right) -> right.createdAtRaw().compareTo(left.createdAtRaw()))
            .toList();

        model.addAttribute("pageTitle", "Domains - OpenMailer");
        model.addAttribute("domainForm", new DomainForm());
        model.addAttribute("domains", domains);
        model.addAttribute("totalDomains", domainService.countByUserId(userId));
        model.addAttribute("verifiedDomains", domainService.countVerifiedDomains(userId));
        model.addAttribute("pendingDomains", domains.stream().filter(domain -> !domain.verified()).count());
        return "domains/list";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("domainForm") DomainForm domainForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            repopulateList(userDetails, model);
            model.addAttribute("pageTitle", "Domains - OpenMailer");
            return "domains/list";
        }

        try {
            Domain domain = new Domain();
            domain.setDomainName(domainForm.getDomainName().trim().toLowerCase());
            domain.setStatus("PENDING");
            domain.setUser(userDetails.getUser());
            domain.setDkimSelector("openmailer");

            DkimKeyGenerationService.DkimKeyPair keys = dkimKeyGenerationService.generateDkimKeys();
            String formattedPublicKey = dkimKeyGenerationService.formatPublicKeyForDns(keys.getPublicKey());
            domain.setDkimPublicKey(formattedPublicKey);
            domain.setDkimPrivateKey(encryptionService.encrypt(keys.getPrivateKey()));
            domain.setSpfRecord(expectedSpfRecord(domain.getDomainName()));
            domain.setDkimRecord(expectedDkimRecord(domain.getDkimSelector(), formattedPublicKey));
            domain.setDmarcRecord(expectedDmarcRecord(domain.getDomainName()));

            Domain saved = domainService.createDomain(domain);
            redirectAttributes.addFlashAttribute("successMessage", "Domain added. Configure the DNS records, then run verification.");
            return "redirect:/domains/" + saved.getId();
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            repopulateList(userDetails, model);
            model.addAttribute("pageTitle", "Domains - OpenMailer");
            return "domains/list";
        } catch (Exception ex) {
            bindingResult.reject("domain.create.failed", "Failed to generate DKIM keys for this domain. Try again.");
            repopulateList(userDetails, model);
            model.addAttribute("pageTitle", "Domains - OpenMailer");
            return "domains/list";
        }
    }

    @GetMapping("/{id}")
    public String view(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        Domain domain = domainService.findByIdAndUserId(id, userDetails.getUser().getId());
        model.addAttribute("pageTitle", domain.getDomainName() + " - Domains - OpenMailer");
        model.addAttribute("domain", toDetailView(domain));
        model.addAttribute("dnsRecords", dnsRecords(domain));
        return "domains/view";
    }

    @PostMapping("/{id}/verify")
    public String verify(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        Domain domain = domainService.findByIdAndUserId(id, userDetails.getUser().getId());

        try {
            String selector = domain.getDkimSelector() != null ? domain.getDkimSelector() : "openmailer";
            DnsVerificationService.DnsVerificationResult verificationResult = dnsVerificationService.verifyDomain(
                domain.getDomainName(),
                selector,
                domain.getSpfRecord() != null ? domain.getSpfRecord() : expectedSpfRecord(domain.getDomainName()),
                domain.getDkimRecord() != null ? domain.getDkimRecord() : expectedDkimRecord(selector, domain.getDkimPublicKey()),
                domain.getDmarcRecord() != null ? domain.getDmarcRecord() : expectedDmarcRecord(domain.getDomainName())
            );

            String status = verificationResult.isAllVerified() ? "VERIFIED" : "FAILED";
            domainService.updateVerificationStatus(
                id,
                userDetails.getUser().getId(),
                status,
                verificationResult.isSpfVerified(),
                verificationResult.isDkimVerified(),
                verificationResult.isDmarcVerified()
            );

            if (verificationResult.isAllVerified()) {
                redirectAttributes.addFlashAttribute("successMessage", "Domain verified successfully.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Verification failed. Recheck the SPF, DKIM, and DMARC records below.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "DNS verification failed: " + ex.getMessage());
        }

        return "redirect:/domains/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        domainService.deleteDomain(id, userDetails.getUser().getId());
        redirectAttributes.addFlashAttribute("successMessage", "Domain deleted successfully.");
        return "redirect:/domains";
    }

    private void repopulateList(CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUser().getId();
        List<DomainListItemView> domains = domainService.findByUserId(userId).stream()
            .map(this::toListItemView)
            .sorted((left, right) -> right.createdAtRaw().compareTo(left.createdAtRaw()))
            .toList();
        model.addAttribute("domains", domains);
        model.addAttribute("totalDomains", domainService.countByUserId(userId));
        model.addAttribute("verifiedDomains", domainService.countVerifiedDomains(userId));
        model.addAttribute("pendingDomains", domains.stream().filter(domain -> !domain.verified()).count());
    }

    private void bindValidationError(BindingResult bindingResult, ValidationException ex) {
        if (ex.getField() != null && !ex.getField().isBlank()) {
            bindingResult.rejectValue(ex.getField(), ex.getField() + ".invalid", ex.getMessage());
            return;
        }
        bindingResult.reject("domain.invalid", ex.getMessage());
    }

    private List<DnsRecordView> dnsRecords(Domain domain) {
        String selector = domain.getDkimSelector() != null ? domain.getDkimSelector() : "openmailer";
        return List.of(
            new DnsRecordView(
                "TXT",
                "@",
                domain.getSpfRecord() != null ? domain.getSpfRecord() : expectedSpfRecord(domain.getDomainName()),
                Boolean.TRUE.equals(domain.getSpfVerified())
            ),
            new DnsRecordView(
                "TXT",
                selector + "._domainkey",
                domain.getDkimRecord() != null ? domain.getDkimRecord() : expectedDkimRecord(selector, domain.getDkimPublicKey()),
                Boolean.TRUE.equals(domain.getDkimVerified())
            ),
            new DnsRecordView(
                "TXT",
                "_dmarc",
                domain.getDmarcRecord() != null ? domain.getDmarcRecord() : expectedDmarcRecord(domain.getDomainName()),
                Boolean.TRUE.equals(domain.getDmarcVerified())
            )
        );
    }

    private String expectedSpfRecord(String domainName) {
        return "v=spf1 include:openmailer.com ~all";
    }

    private String expectedDkimRecord(String selector, String publicKey) {
        return publicKey != null && !publicKey.isBlank()
            ? "v=DKIM1; k=rsa; p=" + publicKey
            : "Generating...";
    }

    private String expectedDmarcRecord(String domainName) {
        return "v=DMARC1; p=quarantine; rua=mailto:dmarc@" + domainName;
    }

    private DomainListItemView toListItemView(Domain domain) {
        boolean verified = "VERIFIED".equalsIgnoreCase(domain.getStatus());
        return new DomainListItemView(
            domain.getId(),
            domain.getDomainName(),
            domain.getStatus(),
            verified,
            verificationSummary(domain),
            formatDateTime(domain.getCreatedAt()),
            domain.getCreatedAt()
        );
    }

    private DomainDetailView toDetailView(Domain domain) {
        boolean verified = "VERIFIED".equalsIgnoreCase(domain.getStatus());
        return new DomainDetailView(
            domain.getId(),
            domain.getDomainName(),
            domain.getStatus(),
            verified,
            Boolean.TRUE.equals(domain.getSpfVerified()),
            Boolean.TRUE.equals(domain.getDkimVerified()),
            Boolean.TRUE.equals(domain.getDmarcVerified()),
            verificationSummary(domain),
            formatDateTime(domain.getCreatedAt()),
            formatDateTime(domain.getUpdatedAt()),
            formatDateTime(domain.getVerifiedAt())
        );
    }

    private String verificationSummary(Domain domain) {
        int verifiedCount = 0;
        if (Boolean.TRUE.equals(domain.getSpfVerified())) {
            verifiedCount++;
        }
        if (Boolean.TRUE.equals(domain.getDkimVerified())) {
            verifiedCount++;
        }
        if (Boolean.TRUE.equals(domain.getDmarcVerified())) {
            verifiedCount++;
        }
        return verifiedCount + "/3 records verified";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMAT) : "Never";
    }

    public static class DomainForm {
        @NotBlank(message = "Domain name is required.")
        @Pattern(
            regexp = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$",
            message = "Enter a valid domain like example.com."
        )
        private String domainName;

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }
    }

    public record DomainListItemView(
        String id,
        String domainName,
        String status,
        boolean verified,
        String verificationSummary,
        String createdAt,
        LocalDateTime createdAtRaw
    ) {
    }

    public record DomainDetailView(
        String id,
        String domainName,
        String status,
        boolean verified,
        boolean spfVerified,
        boolean dkimVerified,
        boolean dmarcVerified,
        String verificationSummary,
        String createdAt,
        String updatedAt,
        String verifiedAt
    ) {
    }

    public record DnsRecordView(String type, String name, String value, boolean verified) {
    }
}
