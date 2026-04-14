package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.repository.ContactRepository;
import com.openmailer.openmailer.repository.DomainRepository;
import com.openmailer.openmailer.repository.EmailCampaignRepository;
import com.openmailer.openmailer.repository.EmailProviderRepository;
import com.openmailer.openmailer.repository.EmailTemplateRepository;
import com.openmailer.openmailer.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ContactRepository contactRepository;
    private final EmailCampaignRepository emailCampaignRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailProviderRepository emailProviderRepository;
    private final DomainRepository domainRepository;

    public DashboardController(
        ContactRepository contactRepository,
        EmailCampaignRepository emailCampaignRepository,
        EmailTemplateRepository emailTemplateRepository,
        EmailProviderRepository emailProviderRepository,
        DomainRepository domainRepository
    ) {
        this.contactRepository = contactRepository;
        this.emailCampaignRepository = emailCampaignRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailProviderRepository = emailProviderRepository;
        this.domainRepository = domainRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("pageTitle", "Dashboard - OpenMailer");
        String displayName = userDetails != null && userDetails.getUser().getFirstName() != null
            && !userDetails.getUser().getFirstName().isBlank()
            ? userDetails.getUser().getFirstName()
            : (userDetails != null ? userDetails.getUser().getUsername() : "there");
        String userId = userDetails != null ? userDetails.getUser().getId() : null;

        model.addAttribute("displayName", displayName);
        model.addAttribute("contactCount", userId != null ? contactRepository.countByUser_Id(userId) : 0L);
        model.addAttribute("campaignCount", userId != null ? emailCampaignRepository.countByUserId(userId) : 0L);
        model.addAttribute("templateCount", userId != null ? emailTemplateRepository.countByUserId(userId) : 0L);
        model.addAttribute("activeProviderCount", userId != null ? emailProviderRepository.countByUserIdAndIsActive(userId, true) : 0L);
        model.addAttribute("verifiedDomainCount", userId != null ? domainRepository.countByUserIdAndStatus(userId, "VERIFIED") : 0L);
        model.addAttribute("draftCampaignCount", userId != null ? emailCampaignRepository.countByUserIdAndStatus(userId, "DRAFT") : 0L);
        model.addAttribute("scheduledCampaignCount", userId != null ? emailCampaignRepository.countByUserIdAndStatus(userId, "SCHEDULED") : 0L);
        model.addAttribute("subscribedContactCount", userId != null ? contactRepository.countByUser_IdAndStatus(userId, "SUBSCRIBED") : 0L);

        return "dashboard";
    }
}
