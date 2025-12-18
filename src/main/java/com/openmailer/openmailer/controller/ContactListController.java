package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.PaginatedResponse;
import com.openmailer.openmailer.dto.contact.ContactResponse;
import com.openmailer.openmailer.dto.list.ContactListRequest;
import com.openmailer.openmailer.dto.list.ContactListResponse;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.contact.ContactListMembershipService;
import com.openmailer.openmailer.service.contact.ContactListService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for contact list management
 */
@RestController
@RequestMapping("/api/v1/lists")
public class ContactListController {

    private static final Logger log = LoggerFactory.getLogger(ContactListController.class);

    private final ContactListService listService;
    private final ContactListMembershipService membershipService;
    private final ContactService contactService;

    public ContactListController(ContactListService listService, ContactListMembershipService membershipService,
                                 ContactService contactService) {
        this.listService = listService;
        this.membershipService = membershipService;
        this.contactService = contactService;
    }

    /**
     * GET /api/v1/lists - List all contact lists
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ContactListResponse>> listContactLists(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ContactList> lists = listService.findByUserId(user.getId(), pageable);

        List<ContactListResponse> responses = lists.getContent().stream()
                .map(ContactListResponse::fromEntity)
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(
                page, size, lists.getTotalElements(), lists.getTotalPages()
        );

        return ResponseEntity.ok(new PaginatedResponse<>(responses, pagination));
    }

    /**
     * GET /api/v1/lists/{id} - Get contact list by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactListResponse>> getContactList(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        ContactList list = listService.findById(id);

        // Check ownership
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", null));
        }

        return ResponseEntity.ok(ApiResponse.success(ContactListResponse.fromEntity(list)));
    }

    /**
     * POST /api/v1/lists - Create new contact list
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ContactListResponse>> createContactList(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ContactListRequest request) {

        ContactList list = new ContactList();
        list.setName(request.getName());
        list.setDescription(request.getDescription());
        list.setDoubleOptInEnabled(request.getDoubleOptInEnabled());
        list.setTotalContacts(0);
        list.setActiveContacts(0);
        list.setUser(user);

        ContactList saved = listService.createContactList(list);

        log.info("Contact list created: {} by user: {}", saved.getId(), user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ContactListResponse.fromEntity(saved), "Contact list created successfully"));
    }

    /**
     * PUT /api/v1/lists/{id} - Update contact list
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactListResponse>> updateContactList(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ContactListRequest request) {

        ContactList list = listService.findById(id);

        // Check ownership
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", null));
        }

        list.setName(request.getName());
        list.setDescription(request.getDescription());
        list.setDoubleOptInEnabled(request.getDoubleOptInEnabled());

        ContactList updated = listService.updateContactList(id, user.getId(), list);

        log.info("Contact list updated: {} by user: {}", updated.getId(), user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(ContactListResponse.fromEntity(updated), "Contact list updated successfully"));
    }

    /**
     * DELETE /api/v1/lists/{id} - Delete contact list
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContactList(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        ContactList list = listService.findById(id);

        // Check ownership
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", null));
        }

        listService.deleteContactList(id, user.getId());

        log.info("Contact list deleted: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(null, "Contact list deleted successfully"));
    }

    /**
     * GET /api/v1/lists/{id}/contacts - Get contacts in list with pagination
     */
    @GetMapping("/{id}/contacts")
    public ResponseEntity<PaginatedResponse<ContactResponse>> getListContacts(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        ContactList list = listService.findById(id);

        // Check ownership
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<com.openmailer.openmailer.model.ContactListMembership> memberships =
                membershipService.findByList(id, pageable);

        List<ContactResponse> responses = memberships.getContent().stream()
                .map(membership -> ContactResponse.fromEntity(membership.getContact()))
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(
                page, size, memberships.getTotalElements(), memberships.getTotalPages()
        );

        return ResponseEntity.ok(new PaginatedResponse<>(responses, pagination));
    }

    /**
     * POST /api/v1/lists/{id}/contacts - Add contacts to list
     */
    @PostMapping("/{id}/contacts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addContactsToList(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {

        ContactList list = listService.findById(id);

        // Check ownership
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", null));
        }

        List<Long> contactIds = request.get("contactIds");
        if (contactIds == null || contactIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "contactIds is required", "contactIds"));
        }

        int added = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Long contactId : contactIds) {
            try {
                Contact contact = contactService.findById(contactId);

                // Verify contact belongs to user
                if (!contact.getUser().getId().equals(user.getId())) {
                    errors.add("Contact " + contactId + " does not belong to you");
                    skipped++;
                    continue;
                }

                // Check if already in list
                if (membershipService.isContactInList(contactId, id)) {
                    skipped++;
                    continue;
                }

                com.openmailer.openmailer.model.ContactListMembership membership =
                        new com.openmailer.openmailer.model.ContactListMembership();
                membership.setContact(contact);
                membership.setContactId(contactId);
                membership.setListId(id);
                membership.setStatus("ACTIVE");
                membershipService.addContactToList(membership);
                added++;

            } catch (Exception e) {
                errors.add("Error adding contact " + contactId + ": " + e.getMessage());
                skipped++;
            }
        }

        // Update list counts
        long totalContacts = membershipService.countByList(id);
        long activeContacts = membershipService.countActiveByList(id);
        listService.updateStatistics(id, user.getId(), (int) totalContacts, (int) activeContacts);

        Map<String, Object> result = new HashMap<>();
        result.put("added", added);
        result.put("skipped", skipped);
        result.put("errors", errors);

        log.info("Added {} contacts to list {} by user: {}", added, id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(result, added + " contacts added to list"));
    }

    /**
     * DELETE /api/v1/lists/{id}/contacts/{contactId} - Remove contact from list
     */
    @DeleteMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<ApiResponse<Void>> removeContactFromList(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long contactId) {

        ContactList list = listService.findById(id);

        // Check ownership
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", null));
        }

        Contact contact = contactService.findById(contactId);

        // Verify contact belongs to user
        if (!contact.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this contact", null));
        }

        membershipService.removeContactFromList(contactId, id);

        // Update list counts
        long totalContacts = membershipService.countByList(id);
        long activeContacts = membershipService.countActiveByList(id);
        listService.updateStatistics(id, user.getId(), (int) totalContacts, (int) activeContacts);

        log.info("Removed contact {} from list {} by user: {}", contactId, id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(null, "Contact removed from list"));
    }

    /**
     * GET /api/v1/lists/{id}/stats - Get list statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getListStats(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        ContactList list = listService.findById(id);

        // Check ownership
        if (!list.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this list", null));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("listId", list.getId());
        stats.put("listName", list.getName());
        stats.put("totalContacts", list.getTotalContacts());
        stats.put("activeContacts", list.getActiveContacts());

        // Get counts by status from membership repository
        stats.put("subscribedCount", 0); // TODO: Implement status-based counting
        stats.put("unsubscribedCount", 0);
        stats.put("bouncedCount", 0);
        stats.put("pendingCount", 0);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
