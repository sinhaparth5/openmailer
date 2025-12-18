package com.openmailer.openmailer.dto.list;

import com.openmailer.openmailer.model.ContactList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for contact list data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactListResponse {
    private Long id;
    private String name;
    private String description;
    private Integer totalContacts;
    private Integer activeContacts;
    private Boolean doubleOptInEnabled;
    private LocalDateTime createdAt;

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
}
