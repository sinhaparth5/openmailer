package com.openmailer.openmailer.dto.list;

import com.openmailer.openmailer.model.ContactList;

import java.time.LocalDateTime;

/**
 * Response DTO for contact list data
 */
public class ContactListResponse {
    private String id;
    private String name;
    private String description;
    private Integer totalContacts;
    private Integer activeContacts;
    private Boolean doubleOptInEnabled;
    private LocalDateTime createdAt;

    public ContactListResponse() {
    }

    public ContactListResponse(String id, String name, String description, Integer totalContacts,
                               Integer activeContacts, Boolean doubleOptInEnabled, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.totalContacts = totalContacts;
        this.activeContacts = activeContacts;
        this.doubleOptInEnabled = doubleOptInEnabled;
        this.createdAt = createdAt;
    }

    public static ContactListResponse fromEntity(ContactList list) {
        ContactListResponse response = new ContactListResponse();
        response.setId(list.getId());
        response.setName(list.getName());
        response.setDescription(list.getDescription());
        response.setTotalContacts(list.getTotalContacts());
        response.setActiveContacts(list.getActiveContacts());
        response.setDoubleOptInEnabled(list.getDoubleOptInEnabled());
        response.setCreatedAt(list.getCreatedAt());
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalContacts() {
        return totalContacts;
    }

    public void setTotalContacts(Integer totalContacts) {
        this.totalContacts = totalContacts;
    }

    public Integer getActiveContacts() {
        return activeContacts;
    }

    public void setActiveContacts(Integer activeContacts) {
        this.activeContacts = activeContacts;
    }

    public Boolean getDoubleOptInEnabled() {
        return doubleOptInEnabled;
    }

    public void setDoubleOptInEnabled(Boolean doubleOptInEnabled) {
        this.doubleOptInEnabled = doubleOptInEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
