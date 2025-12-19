package com.openmailer.openmailer.model;

import com.openmailer.openmailer.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private EmailCampaign campaign;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private CampaignRecipient recipient;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private EmailProvider provider;

    @Column(name = "email_type", nullable = false, length = 50)
    private String emailType;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(length = 500)
    private String subject;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public EmailLog() {
    }

    public EmailLog(User user, String emailType, String recipientEmail, String status) {
        this.user = user;
        this.emailType = emailType;
        this.recipientEmail = recipientEmail;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = IdGenerator.generateId();
        }
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public EmailCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(EmailCampaign campaign) {
        this.campaign = campaign;
    }

    public CampaignRecipient getRecipient() {
        return recipient;
    }

    public void setRecipient(CampaignRecipient recipient) {
        this.recipient = recipient;
    }

    public EmailProvider getProvider() {
        return provider;
    }

    public void setProvider(EmailProvider provider) {
        this.provider = provider;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
