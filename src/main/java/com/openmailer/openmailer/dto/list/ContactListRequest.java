package com.openmailer.openmailer.dto.list;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating contact lists
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactListRequest {

    @NotBlank(message = "List name is required")
    private String name;

    private String description;
    private Boolean doubleOptInEnabled = true;
}
