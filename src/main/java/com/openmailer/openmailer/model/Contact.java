package com.openmailer.openmailer.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "confirmation_token", unique = true)
    private String confirmationToken;

    @Column(name = "confirmation_sent_at")
    private LocalDateTime confirmationSentAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "subscribed_at")
    private LocalDateTime subscribedAt;

    @Column(name = "unsubscribe_token", unique = true)
    private String unsubscribeToken;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "unsubscribe_reason", columnDefinition = "TEXT")
    private String unsubscribeReason;

    @Column(name = "bounce_count")
    private Integer bounceCount = 0;

    @Column(name = "last_bounce_at")
    private LocalDateTime lastBounceAt;

    @Column(name = "bounce_type", length = 50)
    private String bounceType;

    @Column(name = "complaint_count")
    private Integer complaintCount = 0;

    @Column(name = "last_complaint_at")
    private LocalDateTime lastComplaintAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "JSONB")
    private Map<String, Object> customFields;

    @Column(name = "gdpr_consent")
    private Boolean gdprConsent = false;

    @Column(name = "gdpr_consent_date")
    private LocalDateTime gdprConsentDate;

    @Column(name = "gdpr_ip_address", length = 45)
    private String gdprIpAddress;

    @Column(length = 100)
    private String source;

    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Contact() {
    }

    public Contact(User user, String email) {
        this.user = user;
        this.email = email;
        this.status = "PENDING";
        this.emailVerified = false;
        this.bounceCount = 0;
        this.complaintCount = 0;
        this.gdprConsent = false;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public LocalDateTime getConfirmationSentAt() {
        return confirmationSentAt;
    }

    public void setConfirmationSentAt(LocalDateTime confirmationSentAt) {
        this.confirmationSentAt = confirmationSentAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getUnsubscribeToken() {
        return unsubscribeToken;
    }

    public void setUnsubscribeToken(String unsubscribeToken) {
        this.unsubscribeToken = unsubscribeToken;
    }

    public LocalDateTime getUnsubscribedAt() {
        return unsubscribedAt;
    }

    public void setUnsubscribedAt(LocalDateTime unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }

    public String getUnsubscribeReason() {
        return unsubscribeReason;
    }

    public void setUnsubscribeReason(String unsubscribeReason) {
        this.unsubscribeReason = unsubscribeReason;
    }

    public Integer getBounceCount() {
        return bounceCount;
    }

    public void setBounceCount(Integer bounceCount) {
        this.bounceCount = bounceCount;
    }

    public LocalDateTime getLastBounceAt() {
        return lastBounceAt;
    }

    public void setLastBounceAt(LocalDateTime lastBounceAt) {
        this.lastBounceAt = lastBounceAt;
    }

    public String getBounceType() {
        return bounceType;
    }

    public void setBounceType(String bounceType) {
        this.bounceType = bounceType;
    }

    public Integer getComplaintCount() {
        return complaintCount;
    }

    public void setComplaintCount(Integer complaintCount) {
        this.complaintCount = complaintCount;
    }

    public LocalDateTime getLastComplaintAt() {
        return lastComplaintAt;
    }

    public void setLastComplaintAt(LocalDateTime lastComplaintAt) {
        this.lastComplaintAt = lastComplaintAt;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public Boolean getGdprConsent() {
        return gdprConsent;
    }

    public void setGdprConsent(Boolean gdprConsent) {
        this.gdprConsent = gdprConsent;
    }

    public LocalDateTime getGdprConsentDate() {
        return gdprConsentDate;
    }

    public void setGdprConsentDate(LocalDateTime gdprConsentDate) {
        this.gdprConsentDate = gdprConsentDate;
    }

    public String getGdprIpAddress() {
        return gdprIpAddress;
    }

    public void setGdprIpAddress(String gdprIpAddress) {
        this.gdprIpAddress = gdprIpAddress;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
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

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    // Convenience methods for service layer
    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
    }

    public void setLastBouncedAt(LocalDateTime lastBouncedAt) {
        this.lastBounceAt = lastBouncedAt;
    }
}
