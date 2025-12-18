package com.openmailer.openmailer.dto.contact;

import com.openmailer.openmailer.model.Contact;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for contact data
 */
public class ContactResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
    private Map<String, Object> customFields;
    private List<String> tags;
    private Boolean gdprConsent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContactResponse() {
    }

    public ContactResponse(Long id, String email, String firstName, String lastName, String status,
                           Map<String, Object> customFields, List<String> tags, Boolean gdprConsent,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.customFields = customFields;
        this.tags = tags;
        this.gdprConsent = gdprConsent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ContactResponse fromEntity(Contact contact) {
        ContactResponse response = new ContactResponse();
        response.setId(contact.getId());
        response.setEmail(contact.getEmail());
        response.setFirstName(contact.getFirstName());
        response.setLastName(contact.getLastName());
        response.setStatus(contact.getStatus());
        response.setCustomFields(contact.getCustomFields());
        response.setTags(contact.getTags() != null ? Arrays.asList(contact.getTags()) : null);
        response.setGdprConsent(contact.getGdprConsent());
        response.setCreatedAt(contact.getCreatedAt());
        response.setUpdatedAt(contact.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getGdprConsent() {
        return gdprConsent;
    }

    public void setGdprConsent(Boolean gdprConsent) {
        this.gdprConsent = gdprConsent;
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
}
