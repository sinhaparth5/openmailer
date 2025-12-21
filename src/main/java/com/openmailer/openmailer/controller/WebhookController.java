package com.openmailer.openmailer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.repository.ContactRepository;
import com.openmailer.openmailer.service.campaign.TrackingService;
import com.openmailer.openmailer.service.email.BounceProcessingService;
import com.openmailer.openmailer.service.contact.UnsubscribeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for handling webhook events from email providers.
 * Processes bounces, complaints, opens, and clicks from AWS SES, SendGrid, and other providers.
 * These endpoints do NOT require authentication but should be secured by provider signatures in production.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final BounceProcessingService bounceProcessingService;
    private final UnsubscribeService unsubscribeService;
    private final TrackingService trackingService;
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WebhookController(
            BounceProcessingService bounceProcessingService,
            UnsubscribeService unsubscribeService,
            TrackingService trackingService,
            ContactRepository contactRepository) {
        this.bounceProcessingService = bounceProcessingService;
        this.unsubscribeService = unsubscribeService;
        this.trackingService = trackingService;
        this.contactRepository = contactRepository;
    }

    /**
     * Handle AWS SES webhook events.
     * AWS SES sends events via SNS notifications.
     *
     * POST /api/v1/webhooks/aws-ses
     *
     * Event types:
     * - Bounce (hard/soft)
     * - Complaint (spam report)
     * - Delivery
     * - Send
     * - Reject
     * - Open
     * - Click
     *
     * @param payload the SNS notification payload
     * @return success response
     */
    @PostMapping("/aws-ses")
    public ResponseEntity<ApiResponse<String>> handleAwsSes(@RequestBody String payload) {
        log.info("Received AWS SES webhook event");

        try {
            JsonNode json = objectMapper.readTree(payload);

            // Check if this is an SNS subscription confirmation
            String messageType = json.has("Type") ? json.get("Type").asText() : "";

            if ("SubscriptionConfirmation".equals(messageType)) {
                log.info("SNS Subscription confirmation received");
                // In production, you should confirm the subscription
                // by making a GET request to the SubscribeURL
                return ResponseEntity.ok(ApiResponse.success("Subscription confirmation received"));
            }

            // Parse the SNS message
            String message = json.has("Message") ? json.get("Message").asText() : payload;
            JsonNode messageJson = objectMapper.readTree(message);

            String eventType = messageJson.has("eventType") ? messageJson.get("eventType").asText() : "";

            switch (eventType) {
                case "Bounce":
                    handleAwsSesBounce(messageJson);
                    break;
                case "Complaint":
                    handleAwsSesComplaint(messageJson);
                    break;
                case "Open":
                    handleAwsSesOpen(messageJson);
                    break;
                case "Click":
                    handleAwsSesClick(messageJson);
                    break;
                default:
                    log.debug("Unhandled AWS SES event type: {}", eventType);
            }

            return ResponseEntity.ok(ApiResponse.success("Event processed"));

        } catch (Exception e) {
            log.error("Error processing AWS SES webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process webhook"));
        }
    }

    /**
     * Handle SendGrid webhook events.
     *
     * POST /api/v1/webhooks/sendgrid
     *
     * @param payload the webhook payload (array of events)
     * @return success response
     */
    @PostMapping("/sendgrid")
    public ResponseEntity<ApiResponse<String>> handleSendGrid(@RequestBody String payload) {
        log.info("Received SendGrid webhook event");

        try {
            JsonNode events = objectMapper.readTree(payload);

            if (!events.isArray()) {
                log.warn("SendGrid payload is not an array");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid payload format"));
            }

            for (JsonNode event : events) {
                String eventType = event.has("event") ? event.get("event").asText() : "";

                switch (eventType) {
                    case "bounce":
                    case "blocked":
                        handleSendGridBounce(event);
                        break;
                    case "spamreport":
                        handleSendGridComplaint(event);
                        break;
                    case "unsubscribe":
                        handleSendGridUnsubscribe(event);
                        break;
                    case "open":
                        handleSendGridOpen(event);
                        break;
                    case "click":
                        handleSendGridClick(event);
                        break;
                    default:
                        log.debug("Unhandled SendGrid event type: {}", eventType);
                }
            }

            return ResponseEntity.ok(ApiResponse.success("Events processed"));

        } catch (Exception e) {
            log.error("Error processing SendGrid webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process webhook"));
        }
    }

    /**
     * Handle generic SMTP webhook events.
     * This is a custom endpoint for handling bounce notifications from SMTP servers.
     *
     * POST /api/v1/webhooks/smtp
     *
     * @param payload the webhook payload
     * @return success response
     */
    @PostMapping("/smtp")
    public ResponseEntity<ApiResponse<String>> handleSmtp(@RequestBody String payload) {
        log.info("Received SMTP webhook event");

        try {
            JsonNode json = objectMapper.readTree(payload);

            String eventType = json.has("eventType") ? json.get("eventType").asText() : "";
            String email = json.has("email") ? json.get("email").asText() : "";

            if (email.isEmpty()) {
                log.warn("SMTP webhook missing email address");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email address is required"));
            }

            switch (eventType) {
                case "bounce":
                    String bounceType = json.has("bounceType") ? json.get("bounceType").asText() : "hard";
                    String reason = json.has("reason") ? json.get("reason").asText() : "Unknown";
                    processBounceByEmail(email, bounceType, reason);
                    break;
                case "complaint":
                    processComplaintByEmail(email);
                    break;
                default:
                    log.debug("Unhandled SMTP event type: {}", eventType);
            }

            return ResponseEntity.ok(ApiResponse.success("Event processed"));

        } catch (Exception e) {
            log.error("Error processing SMTP webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process webhook"));
        }
    }

    // ============================================
    // AWS SES Event Handlers
    // ============================================

    private void handleAwsSesBounce(JsonNode message) {
        try {
            JsonNode bounce = message.get("bounce");
            String bounceType = bounce.has("bounceType") ? bounce.get("bounceType").asText() : "Permanent";

            JsonNode recipients = bounce.get("bouncedRecipients");
            if (recipients != null && recipients.isArray()) {
                for (JsonNode recipient : recipients) {
                    String email = recipient.has("emailAddress") ? recipient.get("emailAddress").asText() : "";
                    String diagnosticCode = recipient.has("diagnosticCode") ? recipient.get("diagnosticCode").asText() : "Unknown";

                    processBounceByEmail(email, bounceType, diagnosticCode);
                }
            }
        } catch (Exception e) {
            log.error("Error handling AWS SES bounce: {}", e.getMessage(), e);
        }
    }

    private void handleAwsSesComplaint(JsonNode message) {
        try {
            JsonNode complaint = message.get("complaint");
            JsonNode recipients = complaint.get("complainedRecipients");

            if (recipients != null && recipients.isArray()) {
                for (JsonNode recipient : recipients) {
                    String email = recipient.has("emailAddress") ? recipient.get("emailAddress").asText() : "";
                    processComplaintByEmail(email);
                }
            }
        } catch (Exception e) {
            log.error("Error handling AWS SES complaint: {}", e.getMessage(), e);
        }
    }

    private void handleAwsSesOpen(JsonNode message) {
        try {
            JsonNode open = message.get("open");
            // Custom headers can be used to pass tracking ID
            // This is a simplified implementation
            log.debug("AWS SES open event received");
        } catch (Exception e) {
            log.error("Error handling AWS SES open: {}", e.getMessage(), e);
        }
    }

    private void handleAwsSesClick(JsonNode message) {
        try {
            JsonNode click = message.get("click");
            // Custom headers can be used to pass tracking ID
            // This is a simplified implementation
            log.debug("AWS SES click event received");
        } catch (Exception e) {
            log.error("Error handling AWS SES click: {}", e.getMessage(), e);
        }
    }

    // ============================================
    // SendGrid Event Handlers
    // ============================================

    private void handleSendGridBounce(JsonNode event) {
        try {
            String email = event.has("email") ? event.get("email").asText() : "";
            String reason = event.has("reason") ? event.get("reason").asText() : "Unknown";
            String type = event.has("type") ? event.get("type").asText() : "bounce";

            // SendGrid bounce types: "bounce" or "blocked"
            String bounceType = "blocked".equals(type) ? "Permanent" : "Transient";

            processBounceByEmail(email, bounceType, reason);
        } catch (Exception e) {
            log.error("Error handling SendGrid bounce: {}", e.getMessage(), e);
        }
    }

    private void handleSendGridComplaint(JsonNode event) {
        try {
            String email = event.has("email") ? event.get("email").asText() : "";
            processComplaintByEmail(email);
        } catch (Exception e) {
            log.error("Error handling SendGrid complaint: {}", e.getMessage(), e);
        }
    }

    private void handleSendGridUnsubscribe(JsonNode event) {
        try {
            String email = event.has("email") ? event.get("email").asText() : "";

            // Find contact and unsubscribe
            Optional<Contact> contactOpt = findContactByEmail(email);
            if (contactOpt.isPresent()) {
                Contact contact = contactOpt.get();
                if (contact.getUnsubscribeToken() != null) {
                    unsubscribeService.unsubscribe(contact.getUnsubscribeToken(), "Unsubscribed via provider");
                    log.info("Contact {} unsubscribed via SendGrid webhook", email);
                }
            }
        } catch (Exception e) {
            log.error("Error handling SendGrid unsubscribe: {}", e.getMessage(), e);
        }
    }

    private void handleSendGridOpen(JsonNode event) {
        try {
            // SendGrid provides custom args that can include tracking ID
            log.debug("SendGrid open event received");
        } catch (Exception e) {
            log.error("Error handling SendGrid open: {}", e.getMessage(), e);
        }
    }

    private void handleSendGridClick(JsonNode event) {
        try {
            // SendGrid provides custom args that can include tracking ID
            log.debug("SendGrid click event received");
        } catch (Exception e) {
            log.error("Error handling SendGrid click: {}", e.getMessage(), e);
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Process a bounce event by email address.
     */
    private void processBounceByEmail(String email, String bounceType, String reason) {
        Optional<Contact> contactOpt = findContactByEmail(email);

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();

            if ("Permanent".equalsIgnoreCase(bounceType) || "hard".equalsIgnoreCase(bounceType)) {
                bounceProcessingService.processHardBounce(contact.getId(), reason);
                log.info("Hard bounce processed for {}: {}", email, reason);
            } else {
                bounceProcessingService.processSoftBounce(contact.getId(), reason);
                log.info("Soft bounce processed for {}: {}", email, reason);
            }
        } else {
            log.warn("Contact not found for bounce: {}", email);
        }
    }

    /**
     * Process a spam complaint by email address.
     */
    private void processComplaintByEmail(String email) {
        Optional<Contact> contactOpt = findContactByEmail(email);

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();

            // Increment complaint count
            int complaintCount = contact.getComplaintCount() != null ? contact.getComplaintCount() : 0;
            contact.setComplaintCount(complaintCount + 1);

            // Automatically unsubscribe on spam complaint
            if (contact.getUnsubscribeToken() != null) {
                unsubscribeService.unsubscribe(contact.getUnsubscribeToken(), "Spam complaint");
                log.warn("Contact {} unsubscribed due to spam complaint", email);
            }
        } else {
            log.warn("Contact not found for complaint: {}", email);
        }
    }

    /**
     * Find a contact by email address across all users.
     * Note: In production, you may want to scope this by user ID or provider.
     */
    private Optional<Contact> findContactByEmail(String email) {
        // This is a simplified implementation
        // In production, you might want to add an index on email
        // or use a more efficient query
        return contactRepository.findAll().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }
}
