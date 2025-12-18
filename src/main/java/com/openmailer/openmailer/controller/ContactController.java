package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.PaginatedResponse;
import com.openmailer.openmailer.dto.contact.ContactRequest;
import com.openmailer.openmailer.dto.contact.ContactResponse;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.contact.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for contact management
 */
@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * GET /api/v1/contacts - List contacts with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ContactResponse>> listContacts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Contact> contacts;

        if (search != null && !search.isEmpty()) {
            // Search by email, first name, or last name
            contacts = contactService.searchContacts(user.getId(), search, pageable);
        } else if (status != null) {
            // Filter by status
            contacts = contactService.findByStatus(user.getId(), status, pageable);
        } else if (tags != null && !tags.isEmpty()) {
            // Filter by tags - use the first tag for now
            String tag = tags.split(",")[0];
            contacts = contactService.findByTag(user.getId(), tag, pageable);
        } else {
            // All contacts
            contacts = contactService.findByUserId(user.getId(), pageable);
        }

        List<ContactResponse> responses = contacts.getContent().stream()
                .map(ContactResponse::fromEntity)
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(
                page, size, contacts.getTotalElements(), contacts.getTotalPages()
        );

        return ResponseEntity.ok(new PaginatedResponse<>(responses, pagination));
    }

    /**
     * GET /api/v1/contacts/{id} - Get contact by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> getContact(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Contact contact = contactService.findById(id);

        // Check ownership
        if (!contact.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this contact", null));
        }

        return ResponseEntity.ok(ApiResponse.success(ContactResponse.fromEntity(contact)));
    }

    /**
     * POST /api/v1/contacts - Create new contact
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> createContact(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ContactRequest request) {

        // Check if contact already exists
        if (contactService.emailExists(request.getEmail(), user.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("CONTACT_EXISTS", "Contact with this email already exists", "email"));
        }

        Contact contact = new Contact();
        contact.setEmail(request.getEmail());
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());

        // Convert Map<String, String> to Map<String, Object>
        if (request.getCustomFields() != null) {
            contact.setCustomFields(new java.util.HashMap<>(request.getCustomFields()));
        }

        if (request.getTags() != null) {
            contact.setTags(request.getTags().toArray(new String[0]));
        }
        contact.setGdprConsent(request.getGdprConsent());
        contact.setGdprIpAddress(request.getGdprIpAddress());
        contact.setStatus("PENDING");
        contact.setUser(user);

        Contact saved = contactService.createContact(contact);

        log.info("Contact created: {} ({}) by user: {}", saved.getId(), saved.getEmail(), user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ContactResponse.fromEntity(saved), "Contact created successfully"));
    }

    /**
     * PUT /api/v1/contacts/{id} - Update contact
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContact(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ContactRequest request) {

        Contact contact = contactService.findById(id);

        // Check ownership
        if (!contact.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this contact", null));
        }

        // Check if email is being changed and already exists
        if (!contact.getEmail().equals(request.getEmail()) &&
                contactService.emailExists(request.getEmail(), user.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("CONTACT_EXISTS", "Contact with this email already exists", "email"));
        }

        contact.setEmail(request.getEmail());
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());

        // Convert Map<String, String> to Map<String, Object>
        if (request.getCustomFields() != null) {
            contact.setCustomFields(new java.util.HashMap<>(request.getCustomFields()));
        }

        // Convert List<String> to String[]
        if (request.getTags() != null) {
            contact.setTags(request.getTags().toArray(new String[0]));
        }

        contact.setGdprConsent(request.getGdprConsent());

        Contact updated = contactService.updateContact(id, user.getId(), contact);

        log.info("Contact updated: {} ({}) by user: {}", updated.getId(), updated.getEmail(), user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(ContactResponse.fromEntity(updated), "Contact updated successfully"));
    }

    /**
     * DELETE /api/v1/contacts/{id} - Delete contact
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Contact contact = contactService.findById(id);

        // Check ownership
        if (!contact.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this contact", null));
        }

        contactService.deleteContact(id, user.getId());

        log.info("Contact deleted: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(null, "Contact deleted successfully"));
    }

    /**
     * PATCH /api/v1/contacts/{id}/status - Update contact status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContactStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam String status) {

        Contact contact = contactService.findById(id);

        // Check ownership
        if (!contact.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this contact", null));
        }

        contact.setStatus(status);
        Contact updated = contactService.updateContact(id, user.getId(), contact);

        log.info("Contact status updated: {} to {} by user: {}", id, status, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(ContactResponse.fromEntity(updated), "Contact status updated"));
    }

    /**
     * POST /api/v1/contacts/{id}/tags - Add tags to contact
     */
    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<ContactResponse>> addTags(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody List<String> tags) {

        Contact contact = contactService.findById(id);

        // Check ownership
        if (!contact.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this contact", null));
        }

        // Convert String[] to List<String>
        String[] tagsArray = contact.getTags();
        java.util.List<String> currentTags = tagsArray != null
                ? new java.util.ArrayList<>(java.util.Arrays.asList(tagsArray))
                : new java.util.ArrayList<>();

        for (String tag : tags) {
            if (!currentTags.contains(tag)) {
                currentTags.add(tag);
            }
        }

        contact.setTags(currentTags.toArray(new String[0]));
        Contact updated = contactService.updateContact(id, user.getId(), contact);

        log.info("Tags added to contact: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(ContactResponse.fromEntity(updated), "Tags added successfully"));
    }

    /**
     * DELETE /api/v1/contacts/{id}/tags - Remove tags from contact
     */
    @DeleteMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<ContactResponse>> removeTags(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody List<String> tags) {

        Contact contact = contactService.findById(id);

        // Check ownership
        if (!contact.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this contact", null));
        }

        String[] tagsArray = contact.getTags();
        if (tagsArray != null) {
            java.util.List<String> currentTags = new java.util.ArrayList<>(java.util.Arrays.asList(tagsArray));
            currentTags.removeAll(tags);
            contact.setTags(currentTags.toArray(new String[0]));
            Contact updated = contactService.updateContact(id, user.getId(), contact);

            log.info("Tags removed from contact: {} by user: {}", id, user.getEmail());

            return ResponseEntity.ok(ApiResponse.success(ContactResponse.fromEntity(updated), "Tags removed successfully"));
        }

        return ResponseEntity.ok(ApiResponse.success(ContactResponse.fromEntity(contact), "No tags to remove"));
    }
}
