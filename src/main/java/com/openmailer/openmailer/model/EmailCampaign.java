package com.openmailer.openmailer.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_campaigns")
public class EmailCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @Column(nullable = false, length = 50)
    private String status = "DRAFT";

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "total_recipients")
    private Integer totalRecipients = 0;

    @Column(name = "sent_count")
    private Integer sentCount = 0;

    @Column(name = "failed_count")
    private Integer failedCount = 0;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "from_name")
    private String fromName;

    @Column(name = "from_email")
    private String fromEmail;

    @Column(name = "reply_to_email")
    private String replyToEmail;

    @ManyToOne
    @JoinColumn(name = "domain_id")
    private Domain domain;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private EmailProvider provider;

    @ManyToOne
    @JoinColumn(name = "list_id")
    private ContactList contactList;

    @ManyToOne
    @JoinColumn(name = "segment_id")
    private Segment segment;

    @Column(name = "subject_line", length = 500)
    private String subjectLine;

    @Column(name = "preview_text")
    private String previewText;

    @Column(name = "track_opens")
    private Boolean trackOpens = true;

    @Column(name = "track_clicks")
    private Boolean trackClicks = true;

    @Column(name = "open_rate", precision = 5, scale = 2)
    private BigDecimal openRate = BigDecimal.ZERO;

    @Column(name = "click_rate", precision = 5, scale = 2)
    private BigDecimal clickRate = BigDecimal.ZERO;

    @Column(name = "bounce_rate", precision = 5, scale = 2)
    private BigDecimal bounceRate = BigDecimal.ZERO;

    @Column(name = "unsubscribe_count")
    private Integer unsubscribeCount = 0;

    @Column(name = "complaint_count")
    private Integer complaintCount = 0;

    @Column(name = "send_speed")
    private Integer sendSpeed = 100;

    @Column(name = "retry_failed")
    private Boolean retryFailed = true;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public EmailCampaign() {
    }

    public EmailCampaign(String name, EmailTemplate template, User createdBy) {
        this.name = name;
        this.template = template;
        this.createdBy = createdBy;
        this.status = "DRAFT";
        this.totalRecipients = 0;
        this.sentCount = 0;
        this.failedCount = 0;
        this.trackOpens = true;
        this.trackClicks = true;
        this.openRate = BigDecimal.ZERO;
        this.clickRate = BigDecimal.ZERO;
        this.bounceRate = BigDecimal.ZERO;
        this.unsubscribeCount = 0;
        this.complaintCount = 0;
        this.sendSpeed = 100;
        this.retryFailed = true;
        this.maxRetries = 3;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EmailTemplate getTemplate() {
        return template;
    }

    public void setTemplate(EmailTemplate template) {
        this.template = template;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Integer getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(Integer totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getReplyToEmail() {
        return replyToEmail;
    }

    public void setReplyToEmail(String replyToEmail) {
        this.replyToEmail = replyToEmail;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public EmailProvider getProvider() {
        return provider;
    }

    public void setProvider(EmailProvider provider) {
        this.provider = provider;
    }

    public ContactList getContactList() {
        return contactList;
    }

    public void setContactList(ContactList contactList) {
        this.contactList = contactList;
    }

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public String getSubjectLine() {
        return subjectLine;
    }

    public void setSubjectLine(String subjectLine) {
        this.subjectLine = subjectLine;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }

    public Boolean getTrackOpens() {
        return trackOpens;
    }

    public void setTrackOpens(Boolean trackOpens) {
        this.trackOpens = trackOpens;
    }

    public Boolean getTrackClicks() {
        return trackClicks;
    }

    public void setTrackClicks(Boolean trackClicks) {
        this.trackClicks = trackClicks;
    }

    public BigDecimal getOpenRate() {
        return openRate;
    }

    public void setOpenRate(BigDecimal openRate) {
        this.openRate = openRate;
    }

    public BigDecimal getClickRate() {
        return clickRate;
    }

    public void setClickRate(BigDecimal clickRate) {
        this.clickRate = clickRate;
    }

    public BigDecimal getBounceRate() {
        return bounceRate;
    }

    public void setBounceRate(BigDecimal bounceRate) {
        this.bounceRate = bounceRate;
    }

    public Integer getUnsubscribeCount() {
        return unsubscribeCount;
    }

    public void setUnsubscribeCount(Integer unsubscribeCount) {
        this.unsubscribeCount = unsubscribeCount;
    }

    public Integer getComplaintCount() {
        return complaintCount;
    }

    public void setComplaintCount(Integer complaintCount) {
        this.complaintCount = complaintCount;
    }

    public Integer getSendSpeed() {
        return sendSpeed;
    }

    public void setSendSpeed(Integer sendSpeed) {
        this.sendSpeed = sendSpeed;
    }

    public Boolean getRetryFailed() {
        return retryFailed;
    }

    public void setRetryFailed(Boolean retryFailed) {
        this.retryFailed = retryFailed;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // Convenience methods for service layer
    public String getSubject() {
        return this.subjectLine;
    }

    public void setSubject(String subject) {
        this.subjectLine = subject;
    }
}
