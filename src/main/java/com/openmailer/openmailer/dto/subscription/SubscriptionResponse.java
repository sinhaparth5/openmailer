package com.openmailer.openmailer.dto.subscription;

/**
 * Response DTO for subscription operations.
 * Returns the subscription status and relevant information.
 */
public class SubscriptionResponse {

    private String email;
    private String status;
    private String message;
    private boolean success;

    public SubscriptionResponse() {
    }

    public SubscriptionResponse(String email, String status, String message, boolean success) {
        this.email = email;
        this.status = status;
        this.message = message;
        this.success = success;
    }

    public static SubscriptionResponse success(String email, String status, String message) {
        return new SubscriptionResponse(email, status, message, true);
    }

    public static SubscriptionResponse error(String email, String message) {
        return new SubscriptionResponse(email, null, message, false);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
