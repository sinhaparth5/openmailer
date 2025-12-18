package com.openmailer.openmailer.service.email;

import com.openmailer.openmailer.model.ProviderType;

import java.util.List;
import java.util.Map;

/**
 * Interface for email sending implementations
 * Implemented by AWS SES, SendGrid, and SMTP providers
 */
public interface EmailSender {

    /**
     * Sends an email using the configured provider
     *
     * @param emailRequest The email request containing all necessary data
     * @return EmailSendResponse with send status and message ID
     * @throws EmailSendException if sending fails
     */
    EmailSendResponse send(EmailSendRequest emailRequest) throws EmailSendException;

    /**
     * Validates that the provider is properly configured
     *
     * @return true if provider is ready to send emails
     */
    boolean isConfigured();

    /**
     * Gets the provider type this sender handles
     *
     * @return The provider type
     */
    ProviderType getProviderType();

    /**
     * Request object for sending emails
     */
    class EmailSendRequest {
        private String to;
        private String from;
        private String fromName;
        private String replyTo;
        private String subject;
        private String htmlBody;
        private String textBody;
        private List<String> cc;
        private List<String> bcc;
        private Map<String, String> headers;
        private List<Attachment> attachments;

        // Tracking
        private String trackingId;
        private boolean trackOpens;
        private boolean trackClicks;

        public EmailSendRequest() {
        }

        public EmailSendRequest(String to, String from, String subject, String htmlBody) {
            this.to = to;
            this.from = from;
            this.subject = subject;
            this.htmlBody = htmlBody;
        }

        // Getters and Setters
        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getReplyTo() {
            return replyTo;
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getHtmlBody() {
            return htmlBody;
        }

        public void setHtmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
        }

        public String getTextBody() {
            return textBody;
        }

        public void setTextBody(String textBody) {
            this.textBody = textBody;
        }

        public List<String> getCc() {
            return cc;
        }

        public void setCc(List<String> cc) {
            this.cc = cc;
        }

        public List<String> getBcc() {
            return bcc;
        }

        public void setBcc(List<String> bcc) {
            this.bcc = bcc;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public List<Attachment> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<Attachment> attachments) {
            this.attachments = attachments;
        }

        public String getTrackingId() {
            return trackingId;
        }

        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
        }

        public boolean isTrackOpens() {
            return trackOpens;
        }

        public void setTrackOpens(boolean trackOpens) {
            this.trackOpens = trackOpens;
        }

        public boolean isTrackClicks() {
            return trackClicks;
        }

        public void setTrackClicks(boolean trackClicks) {
            this.trackClicks = trackClicks;
        }
    }

    /**
     * Response object for email sending
     */
    class EmailSendResponse {
        private boolean success;
        private String messageId;
        private String errorMessage;
        private String providerResponse;

        public EmailSendResponse(boolean success, String messageId) {
            this.success = success;
            this.messageId = messageId;
        }

        public EmailSendResponse(boolean success, String messageId, String errorMessage) {
            this.success = success;
            this.messageId = messageId;
            this.errorMessage = errorMessage;
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getProviderResponse() {
            return providerResponse;
        }

        public void setProviderResponse(String providerResponse) {
            this.providerResponse = providerResponse;
        }
    }

    /**
     * Email attachment
     */
    class Attachment {
        private String filename;
        private String contentType;
        private byte[] content;

        public Attachment() {
        }

        public Attachment(String filename, String contentType, byte[] content) {
            this.filename = filename;
            this.contentType = contentType;
            this.content = content;
        }

        // Getters and Setters
        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }
    }

    /**
     * Exception thrown when email sending fails
     */
    class EmailSendException extends Exception {
        private String providerError;

        public EmailSendException(String message) {
            super(message);
        }

        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }

        public EmailSendException(String message, String providerError) {
            super(message);
            this.providerError = providerError;
        }

        public String getProviderError() {
            return providerError;
        }
    }
}
