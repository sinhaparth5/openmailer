package com.openmailer.openmailer.dto.list;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating/updating contact lists
 */
public class ContactListRequest {

    @NotBlank(message = "List name is required")
    private String name;

    private String description;
    private Boolean doubleOptInEnabled = true;

    public ContactListRequest() {
    }

    public ContactListRequest(String name, String description, Boolean doubleOptInEnabled) {
        this.name = name;
        this.description = description;
        this.doubleOptInEnabled = doubleOptInEnabled;
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

    public Boolean getDoubleOptInEnabled() {
        return doubleOptInEnabled;
    }

    public void setDoubleOptInEnabled(Boolean doubleOptInEnabled) {
        this.doubleOptInEnabled = doubleOptInEnabled;
    }
}
