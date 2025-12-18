package com.openmailer.openmailer.dto.contact;

import com.openmailer.openmailer.model.Contact;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for contact data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Contact.ContactStatus status;
    private Map<String, String> customFields;
    private List<String> tags;
    private Boolean gdprConsent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContactResponse fromEntity(Contact contact) {
        ContactResponse response = new ContactResponse();
        response.setId(contact.getId());
        response.setEmail(contact.getEmail());
        response.setFirstName(contact.getFirstName());
        response.setLastName(contact.getLastName());
        response.setStatus(contact.getStatus());
        response.setCustomFields(contact.getCustomFields());
        response.setTags(contact.getTags());
        response.setGdprConsent(contact.getGdprConsent());
        response.setCreatedAt(contact.getCreatedAt());
        response.setUpdatedAt(contact.getUpdatedAt());
        return response;
    }
}
