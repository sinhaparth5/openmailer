package com.openmailer.openmailer.model;

import com.openmailer.openmailer.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_links")
public class CampaignLink {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private EmailCampaign campaign;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(name = "click_count")
    private Integer clickCount = 0;

    @Column(name = "unique_click_count")
    private Integer uniqueClickCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public CampaignLink() {
    }

    public CampaignLink(EmailCampaign campaign, String originalUrl, String shortCode) {
        this.campaign = campaign;
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.clickCount = 0;
        this.uniqueClickCount = 0;
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

    public EmailCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(EmailCampaign campaign) {
        this.campaign = campaign;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Integer getUniqueClickCount() {
        return uniqueClickCount;
    }

    public void setUniqueClickCount(Integer uniqueClickCount) {
        this.uniqueClickCount = uniqueClickCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
