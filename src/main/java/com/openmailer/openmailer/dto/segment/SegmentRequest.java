package com.openmailer.openmailer.dto.segment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO for creating or updating a segment.
 */
public class SegmentRequest {

    @NotBlank(message = "Segment name is required")
    @Size(max = 255, message = "Segment name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Conditions are required")
    private Map<String, Object> conditions;

    private Boolean isDynamic;

    private String listId;

    // Constructors
    public SegmentRequest() {
    }

    public SegmentRequest(String name, String description, Map<String, Object> conditions) {
        this.name = name;
        this.description = description;
        this.conditions = conditions;
    }

    // Getters and Setters
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

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }
}
