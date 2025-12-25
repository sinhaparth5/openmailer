package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.PaginatedResponse;
import com.openmailer.openmailer.dto.response.common.ApiResponse;
import com.openmailer.openmailer.dto.segment.SegmentRequest;
import com.openmailer.openmailer.dto.segment.SegmentResponse;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.Segment;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.contact.ContactListService;
import com.openmailer.openmailer.service.contact.ContactService;
import com.openmailer.openmailer.service.contact.SegmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for segment management.
 * Provides endpoints for creating, updating, and managing contact segments.
 */
@RestController
@RequestMapping("/api/v1/segments")
public class SegmentController {

    private final SegmentService segmentService;
    private final ContactListService contactListService;
    private final ContactService contactService;

    @Autowired
    public SegmentController(SegmentService segmentService,
                            ContactListService contactListService,
                            ContactService contactService) {
        this.segmentService = segmentService;
        this.contactListService = contactListService;
        this.contactService = contactService;
    }

    /**
     * List all segments for the authenticated user.
     *
     * @param userDetails the authenticated user
     * @param page the page number (default: 0)
     * @param size the page size (default: 20)
     * @param type filter by type: "dynamic" or "static" (optional)
     * @param search search by name (optional)
     * @return paginated list of segments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<SegmentResponse>>> listSegments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        User user = userDetails.getUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Segment> segmentPage;

        if (search != null && !search.isBlank()) {
            segmentPage = segmentService.searchByName(user.getId(), search, pageable);
        } else if (type != null && !type.isBlank()) {
            Boolean isDynamic = "dynamic".equalsIgnoreCase(type);
            segmentPage = segmentService.findByType(user.getId(), isDynamic, pageable);
        } else {
            segmentPage = segmentService.findByUserId(user.getId(), pageable);
        }

        List<SegmentResponse> responses = segmentPage.getContent().stream()
                .map(SegmentResponse::fromEntity)
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo paginationInfo = new PaginatedResponse.PaginationInfo(
                segmentPage.getNumber(),
                segmentPage.getSize(),
                segmentPage.getTotalElements(),
                segmentPage.getTotalPages()
        );

        PaginatedResponse<SegmentResponse> paginatedResponse = new PaginatedResponse<>(
                responses,
                paginationInfo
        );

        return ResponseEntity.ok(ApiResponse.success(paginatedResponse));
    }

    /**
     * Get a specific segment by ID.
     *
     * @param userDetails the authenticated user
     * @param id the segment ID
     * @return the segment details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SegmentResponse>> getSegment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id) {

        User user = userDetails.getUser();
        Segment segment = segmentService.findByIdAndUserId(id, user.getId());
        SegmentResponse response = SegmentResponse.fromEntity(segment);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Create a new segment.
     *
     * @param userDetails the authenticated user
     * @param request the segment creation request
     * @return the created segment
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SegmentResponse>> createSegment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SegmentRequest request) {

        User user = userDetails.getUser();

        // Create segment entity
        Segment segment = new Segment();
        segment.setUser(user);
        segment.setName(request.getName());
        segment.setDescription(request.getDescription());
        segment.setConditions(request.getConditions());
        segment.setIsDynamic(request.getIsDynamic() != null ? request.getIsDynamic() : true);

        // Set contact list if provided
        if (request.getListId() != null && !request.getListId().isBlank()) {
            ContactList contactList = contactListService.findByIdAndUserId(request.getListId(), user.getId());
            segment.setContactList(contactList);
        }

        Segment createdSegment = segmentService.createSegment(segment);
        SegmentResponse response = SegmentResponse.fromEntity(createdSegment);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update an existing segment.
     *
     * @param userDetails the authenticated user
     * @param id the segment ID
     * @param request the update request
     * @return the updated segment
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SegmentResponse>> updateSegment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody SegmentRequest request) {

        User user = userDetails.getUser();

        // Create update entity
        Segment updateData = new Segment();
        updateData.setName(request.getName());
        updateData.setDescription(request.getDescription());
        updateData.setConditions(request.getConditions());
        updateData.setIsDynamic(request.getIsDynamic());

        // Set contact list if provided
        if (request.getListId() != null && !request.getListId().isBlank()) {
            ContactList contactList = contactListService.findByIdAndUserId(request.getListId(), user.getId());
            updateData.setContactList(contactList);
        }

        Segment updatedSegment = segmentService.updateSegment(id, user.getId(), updateData);
        SegmentResponse response = SegmentResponse.fromEntity(updatedSegment);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a segment.
     *
     * @param userDetails the authenticated user
     * @param id the segment ID
     * @return success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSegment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id) {

        User user = userDetails.getUser();
        segmentService.deleteSegment(id, user.getId());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get contacts matching a segment.
     * Note: This is a simplified implementation. A full implementation would
     * require a dynamic query builder to evaluate segment conditions.
     *
     * @param userDetails the authenticated user
     * @param id the segment ID
     * @param page the page number (default: 0)
     * @param size the page size (default: 50)
     * @return paginated list of contacts
     */
    @GetMapping("/{id}/contacts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSegmentContacts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        User user = userDetails.getUser();
        Segment segment = segmentService.findByIdAndUserId(id, user.getId());

        // For now, return contacts from the associated list if available
        // A full implementation would evaluate the segment conditions
        Map<String, Object> result = new HashMap<>();
        result.put("segmentId", segment.getId());
        result.put("segmentName", segment.getName());
        result.put("isDynamic", segment.getIsDynamic());
        result.put("conditions", segment.getConditions());

        // For now, return all user's contacts as this is a simplified implementation
        // A full implementation would evaluate the segment conditions dynamically
        result.put("message", "Segment condition evaluation not yet implemented. " +
                "Showing all user contacts. Full implementation requires dynamic query builder.");

        Pageable pageable = PageRequest.of(page, size);
        Page<Contact> contacts = contactService.findByUserId(user.getId(), pageable);

        result.put("contacts", contacts.getContent());
        result.put("totalContacts", contacts.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", contacts.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Evaluate segment conditions and return matching count.
     * This endpoint can be used to test segment conditions before saving.
     *
     * @param userDetails the authenticated user
     * @param id the segment ID
     * @return evaluation results with contact count
     */
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluateSegment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id) {

        User user = userDetails.getUser();
        Segment segment = segmentService.findByIdAndUserId(id, user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("segmentId", segment.getId());
        result.put("segmentName", segment.getName());
        result.put("conditions", segment.getConditions());
        result.put("isDynamic", segment.getIsDynamic());

        // For now, return cached count or list size
        // Full implementation would evaluate conditions against database
        if (segment.getCachedCount() != null && segment.getCachedCount() > 0) {
            result.put("matchingContacts", segment.getCachedCount());
            result.put("lastCalculatedAt", segment.getLastCalculatedAt());
            result.put("source", "cached");
        } else if (segment.getContactList() != null) {
            int count = segment.getContactList().getTotalContacts();
            result.put("matchingContacts", count);
            result.put("source", "list_total");
        } else {
            // Count all user contacts as estimate
            long totalContacts = contactService.countByUserId(user.getId());
            result.put("matchingContacts", totalContacts);
            result.put("source", "user_total_estimate");
            result.put("message", "Full condition evaluation not yet implemented. " +
                    "Showing total user contacts. Full implementation requires dynamic query builder.");
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
