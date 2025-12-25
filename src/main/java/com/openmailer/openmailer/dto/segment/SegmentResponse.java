package com.openmailer.openmailer.dto.segment;

import com.openmailer.openmailer.model.Segment;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for segment responses.
 */
public class SegmentResponse {

    private String id;
    private String userId;
    private String listId;
    private String listName;
    private String name;
    private String description;
    private Map<String, Object> conditions;
    private Boolean isDynamic;
    private Integer cachedCount;
    private LocalDateTime lastCalculatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public SegmentResponse() {
    }

    /**
     * Create response from Segment entity.
     *
     * @param segment the segment entity
     * @return the segment response
     */
    public static SegmentResponse fromEntity(Segment segment) {
        SegmentResponse response = new SegmentResponse();
        response.setId(segment.getId());
        response.setUserId(segment.getUser() != null ? segment.getUser().getId() : null);
        response.setListId(segment.getContactList() != null ? segment.getContactList().getId() : null);
        response.setListName(segment.getContactList() != null ? segment.getContactList().getName() : null);
        response.setName(segment.getName());
        response.setDescription(segment.getDescription());
        response.setConditions(segment.getConditions());
        response.setIsDynamic(segment.getIsDynamic());
        response.setCachedCount(segment.getCachedCount());
        response.setLastCalculatedAt(segment.getLastCalculatedAt());
        response.setCreatedAt(segment.getCreatedAt());
        response.setUpdatedAt(segment.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
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

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public Boolean getIsDynamic() {
        return isDynamic;
    }

    public void setIsDynamic(Boolean isDynamic) {
        this.isDynamic = isDynamic;
    }

    public Integer getCachedCount() {
        return cachedCount;
    }

    public void setCachedCount(Integer cachedCount) {
        this.cachedCount = cachedCount;
    }

    public LocalDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
