package com.openmailer.openmailer.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openmailer.openmailer.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "email_providers")
public class EmailProvider {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false, length = 50)
    private String userId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "domain_id")
    private Domain domain;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    @Column(length = 50)
    private String status = "ACTIVE";

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    @Column(name = "emails_sent")
    private Integer emailsSent = 0;

    @Column(name = "emails_failed")
    private Integer emailsFailed = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public EmailProvider() {
    }

    public EmailProvider(User user, ProviderType providerType, String providerName, String configuration) {
        this.user = user;
        this.providerType = providerType;
        this.providerName = providerName;
        this.configuration = configuration;
        this.isActive = true;
        this.isDefault = false;
        this.priority = 0;
        this.status = "ACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = IdGenerator.generateId();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(ProviderType providerType) {
        this.providerType = providerType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    /**
     * Parses the JSON configuration string into a Map
     *
     * @return Map of configuration key-value pairs
     */
    public Map<String, String> getConfigurationMap() {
        if (configuration == null || configuration.isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(configuration, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse configuration JSON", e);
        }
    }

    /**
     * Sets the configuration from a Map by converting it to JSON
     *
     * @param configMap The configuration map
     */
    public void setConfigurationMap(Map<String, String> configMap) {
        if (configMap == null || configMap.isEmpty()) {
            this.configuration = "{}";
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.configuration = mapper.writeValueAsString(configMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize configuration to JSON", e);
        }
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Integer monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getLastErrorAt() {
        return lastErrorAt;
    }

    public void setLastErrorAt(LocalDateTime lastErrorAt) {
        this.lastErrorAt = lastErrorAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getEmailsSent() {
        return emailsSent;
    }

    public void setEmailsSent(Integer emailsSent) {
        this.emailsSent = emailsSent;
    }

    public Integer getEmailsFailed() {
        return emailsFailed;
    }

    public void setEmailsFailed(Integer emailsFailed) {
        this.emailsFailed = emailsFailed;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    // Convenience method for service layer
    public String getName() {
        return this.providerName;
    }

    public void setName(String name) {
        this.providerName = name;
    }
}

