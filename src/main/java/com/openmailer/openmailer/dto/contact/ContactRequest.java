package com.openmailer.openmailer.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating/updating contacts
 */
public class ContactRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String firstName;
    private String lastName;
    private Map<String, String> customFields;
    private List<String> tags;
    private Boolean gdprConsent;
    private String gdprIpAddress;

    public ContactRequest() {
    }

    public ContactRequest(String email, String firstName, String lastName, Map<String, String> customFields,
                          List<String> tags, Boolean gdprConsent, String gdprIpAddress) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.customFields = customFields;
        this.tags = tags;
        this.gdprConsent = gdprConsent;
        this.gdprIpAddress = gdprIpAddress;
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

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
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

    public String getGdprIpAddress() {
        return gdprIpAddress;
    }

    public void setGdprIpAddress(String gdprIpAddress) {
        this.gdprIpAddress = gdprIpAddress;
    }
}
