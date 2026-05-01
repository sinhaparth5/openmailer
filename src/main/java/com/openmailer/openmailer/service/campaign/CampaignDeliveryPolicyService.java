package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CampaignDeliveryPolicyService {

    private final int sharedSenderLimit;
    private final String sharedSenderEmail;
    private final String sharedSenderName;

    public CampaignDeliveryPolicyService(
        @Value("${app.free-plan.shared-sender-limit:10}") int sharedSenderLimit,
        @Value("${app.mail.from:}") String sharedSenderEmail,
        @Value("${app.free-plan.shared-sender-name:OpenMailer}") String sharedSenderName
    ) {
        this.sharedSenderLimit = sharedSenderLimit;
        this.sharedSenderEmail = sharedSenderEmail != null ? sharedSenderEmail.trim() : "";
        this.sharedSenderName = sharedSenderName != null ? sharedSenderName.trim() : "OpenMailer";
    }

    public boolean usesSharedSender(String domainId, String providerId) {
        return isBlank(domainId) && isBlank(providerId);
    }

    public boolean usesSharedSender(EmailCampaign campaign) {
        return campaign.getDomain() == null && campaign.getProvider() == null;
    }

    public boolean isFreePlan(User user) {
        if (user == null || user.getSubscriptionPlan() == null || user.getSubscriptionPlan().isBlank()) {
            return true;
        }
        return "FREE".equalsIgnoreCase(user.getSubscriptionPlan());
    }

    public boolean sharedSenderConfigured() {
        return !sharedSenderEmail.isBlank();
    }

    public boolean canUseSharedSender(User user) {
        return isFreePlan(user) && sharedSenderConfigured();
    }

    public int getSharedSenderLimit() {
        return sharedSenderLimit;
    }

    public int getSharedSenderRemaining(User user) {
        int sent = user != null && user.getMonthlyEmailsSent() != null ? user.getMonthlyEmailsSent() : 0;
        return Math.max(sharedSenderLimit - sent, 0);
    }

    public String getSharedSenderEmail() {
        return sharedSenderEmail;
    }

    public String getSharedSenderName() {
        return sharedSenderName;
    }

    public void validateDraftConfiguration(User user, String domainId, String providerId) {
        boolean sharedSenderMode = usesSharedSender(domainId, providerId);
        boolean customSenderMode = !isBlank(domainId) && !isBlank(providerId);

        if (sharedSenderMode) {
            if (!canUseSharedSender(user)) {
                throw new ValidationException(
                    "A verified domain and active provider are required, or the OpenMailer shared sender must be enabled for your free plan."
                );
            }
            return;
        }

        if (!customSenderMode) {
            throw new ValidationException("Select both a verified domain and an active provider, or leave both empty to use the OpenMailer shared sender.");
        }
    }

    public void validateSharedSenderQuota(User user, int recipientCount) {
        if (!canUseSharedSender(user)) {
            throw new ValidationException("The OpenMailer shared sender is not available for this account.");
        }

        int remaining = getSharedSenderRemaining(user);
        if (remaining <= 0) {
            throw new ValidationException(
                "Your free shared-sender quota is exhausted. Add your own verified domain and provider to continue sending."
            );
        }

        if (recipientCount > remaining) {
            throw new ValidationException(
                "This send targets " + recipientCount + " recipients, but only " + remaining + " free shared-sender emails remain for your account."
            );
        }
    }

    public boolean dependenciesReady(User user, boolean hasTemplates, boolean hasLists, boolean hasDomains, boolean hasProviders) {
        if (canUseSharedSender(user)) {
            return hasTemplates && hasLists;
        }
        return hasTemplates && hasLists && hasDomains && hasProviders;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
