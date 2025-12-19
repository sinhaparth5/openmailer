package com.openmailer.openmailer.model;

import com.openmailer.openmailer.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_clicks")
public class CampaignClick {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private EmailCampaign campaign;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private CampaignRecipient recipient;

    @ManyToOne
    @JoinColumn(name = "link_id", nullable = false)
    private CampaignLink link;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // Constructors
    public CampaignClick() {
    }

    public CampaignClick(EmailCampaign campaign, CampaignRecipient recipient, CampaignLink link) {
        this.campaign = campaign;
        this.recipient = recipient;
        this.link = link;
        this.clickedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = IdGenerator.generateId();
        }
        if (clickedAt == null) {
            clickedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public CampaignLink getLink() {
        return link;
    }

    public void setLink(CampaignLink link) {
        this.link = link;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
