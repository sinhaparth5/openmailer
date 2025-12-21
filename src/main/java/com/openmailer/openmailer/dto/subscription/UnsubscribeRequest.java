package com.openmailer.openmailer.dto.subscription;

/**
 * Request DTO for unsubscribe requests.
 * Optionally includes a reason for unsubscribing.
 */
public class UnsubscribeRequest {

    private String reason;
    private String listId; // Optional: unsubscribe from specific list only

    public UnsubscribeRequest() {
    }

    public UnsubscribeRequest(String reason, String listId) {
        this.reason = reason;
        this.listId = listId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }
}
