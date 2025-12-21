package com.openmailer.openmailer.dto.subscription;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for public subscription requests.
 * Used for double opt-in subscription flow.
 */
public class SubscribeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String firstName;
    private String lastName;

    @NotBlank(message = "List ID is required")
    private String listId;

    private String source;

    public SubscribeRequest() {
    }

    public SubscribeRequest(String email, String firstName, String lastName, String listId, String source) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.listId = listId;
        this.source = source;
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

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
