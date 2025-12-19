package com.openmailer.openmailer.model;

import com.openmailer.openmailer.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "domains")
public class Domain {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false, length = 50)
    private String userId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "domain_name", nullable = false, unique = true)
    private String domainName;

    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    // SPF Record
    @Column(name = "spf_record", columnDefinition = "TEXT")
    private String spfRecord;

    @Column(name = "spf_verified")
    private Boolean spfVerified = false;

    @Column(name = "spf_last_checked_at")
    private LocalDateTime spfLastCheckedAt;

    // DKIM Record
    @Column(name = "dkim_selector", length = 100)
    private String dkimSelector = "openmailer";

    @Column(name = "dkim_record", columnDefinition = "TEXT")
    private String dkimRecord;

    @Column(name = "dkim_public_key", columnDefinition = "TEXT")
    private String dkimPublicKey;

    @Column(name = "dkim_private_key", columnDefinition = "TEXT")
    private String dkimPrivateKey;

    @Column(name = "dkim_verified")
    private Boolean dkimVerified = false;

    @Column(name = "dkim_last_checked_at")
    private LocalDateTime dkimLastCheckedAt;

    // DMARC Record
    @Column(name = "dmarc_record", columnDefinition = "TEXT")
    private String dmarcRecord;

    @Column(name = "dmarc_verified")
    private Boolean dmarcVerified = false;

    @Column(name = "dmarc_last_checked_at")
    private LocalDateTime dmarcLastCheckedAt;

    // Verification
    @Column(name = "verification_token", unique = true)
    private String verificationToken;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Domain() {
    }

    public Domain(User user, String domainName) {
        this.user = user;
        this.domainName = domainName;
        this.status = "PENDING";
        this.spfVerified = false;
        this.dkimSelector = "openmailer";
        this.dkimVerified = false;
        this.dmarcVerified = false;
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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpfRecord() {
        return spfRecord;
    }

    public void setSpfRecord(String spfRecord) {
        this.spfRecord = spfRecord;
    }

    public Boolean getSpfVerified() {
        return spfVerified;
    }

    public void setSpfVerified(Boolean spfVerified) {
        this.spfVerified = spfVerified;
    }

    public LocalDateTime getSpfLastCheckedAt() {
        return spfLastCheckedAt;
    }

    public void setSpfLastCheckedAt(LocalDateTime spfLastCheckedAt) {
        this.spfLastCheckedAt = spfLastCheckedAt;
    }

    public String getDkimSelector() {
        return dkimSelector;
    }

    public void setDkimSelector(String dkimSelector) {
        this.dkimSelector = dkimSelector;
    }

    public String getDkimPublicKey() {
        return dkimPublicKey;
    }

    public void setDkimPublicKey(String dkimPublicKey) {
        this.dkimPublicKey = dkimPublicKey;
    }

    public String getDkimPrivateKey() {
        return dkimPrivateKey;
    }

    public void setDkimPrivateKey(String dkimPrivateKey) {
        this.dkimPrivateKey = dkimPrivateKey;
    }

    public Boolean getDkimVerified() {
        return dkimVerified;
    }

    public void setDkimVerified(Boolean dkimVerified) {
        this.dkimVerified = dkimVerified;
    }

    public LocalDateTime getDkimLastCheckedAt() {
        return dkimLastCheckedAt;
    }

    public void setDkimLastCheckedAt(LocalDateTime dkimLastCheckedAt) {
        this.dkimLastCheckedAt = dkimLastCheckedAt;
    }

    public String getDmarcRecord() {
        return dmarcRecord;
    }

    public void setDmarcRecord(String dmarcRecord) {
        this.dmarcRecord = dmarcRecord;
    }

    public Boolean getDmarcVerified() {
        return dmarcVerified;
    }

    public void setDmarcVerified(Boolean dmarcVerified) {
        this.dmarcVerified = dmarcVerified;
    }

    public LocalDateTime getDmarcLastCheckedAt() {
        return dmarcLastCheckedAt;
    }

    public void setDmarcLastCheckedAt(LocalDateTime dmarcLastCheckedAt) {
        this.dmarcLastCheckedAt = dmarcLastCheckedAt;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
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

    public String getDkimRecord() {
        return dkimRecord;
    }

    public void setDkimRecord(String dkimRecord) {
        this.dkimRecord = dkimRecord;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
