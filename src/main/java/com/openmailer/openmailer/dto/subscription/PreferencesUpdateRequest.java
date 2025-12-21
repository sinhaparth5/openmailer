package com.openmailer.openmailer.dto.subscription;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating subscriber preferences.
 * Allows contacts to update their information and list subscriptions.
 */
public class PreferencesUpdateRequest {

    private String firstName;
    private String lastName;
    private List<String> listIds;
    private String emailFrequency;
    private List<String> topics;
    private Map<String, Object> customFields;

    public PreferencesUpdateRequest() {
    }

    public PreferencesUpdateRequest(String firstName, String lastName, List<String> listIds,
                                  String emailFrequency, List<String> topics, Map<String, Object> customFields) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.listIds = listIds;
        this.emailFrequency = emailFrequency;
        this.topics = topics;
        this.customFields = customFields;
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

    public List<String> getListIds() {
        return listIds;
    }

    public void setListIds(List<String> listIds) {
        this.listIds = listIds;
    }

    public String getEmailFrequency() {
        return emailFrequency;
    }

    public void setEmailFrequency(String emailFrequency) {
        this.emailFrequency = emailFrequency;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
