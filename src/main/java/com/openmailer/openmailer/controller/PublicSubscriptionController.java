package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.subscription.PreferencesUpdateRequest;
import com.openmailer.openmailer.dto.subscription.SubscribeRequest;
import com.openmailer.openmailer.dto.subscription.SubscriptionResponse;
import com.openmailer.openmailer.dto.subscription.UnsubscribeRequest;
import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.service.contact.PreferenceCenterService;
import com.openmailer.openmailer.service.contact.SubscriptionService;
import com.openmailer.openmailer.service.contact.UnsubscribeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public controller for subscription management.
 * These endpoints do NOT require authentication and are used for:
 * - Public subscription forms
 * - Email confirmation
 * - Unsubscribe links
 * - Preference center
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(PublicSubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final UnsubscribeService unsubscribeService;
    private final PreferenceCenterService preferenceCenterService;

    @Autowired
    public PublicSubscriptionController(
            SubscriptionService subscriptionService,
            UnsubscribeService unsubscribeService,
            PreferenceCenterService preferenceCenterService) {
        this.subscriptionService = subscriptionService;
        this.unsubscribeService = unsubscribeService;
        this.preferenceCenterService = preferenceCenterService;
    }

    /**
     * Handle public subscription requests (double opt-in).
     * Creates a contact with PENDING status and sends confirmation email.
     *
     * POST /api/v1/public/subscribe
     *
     * @param request the subscription request
     * @param httpRequest the HTTP request (for IP address)
     * @return subscription response
     */
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Public subscription request for email: {}", request.getEmail());

        try {
            // Get IP address for GDPR compliance
            String ipAddress = getClientIpAddress(httpRequest);

            // Set default source if not provided
            String source = request.getSource() != null ? request.getSource() : "public-api";

            // Process subscription
            Contact contact = subscriptionService.subscribe(
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getListId(),
                    ipAddress,
                    source
            );

            SubscriptionResponse response = SubscriptionResponse.success(
                    contact.getEmail(),
                    contact.getStatus(),
                    "Subscription request received. Please check your email to confirm."
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (ValidationException e) {
            log.warn("Validation error for subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error processing subscription for {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Subscription failed"));
        }
    }

    /**
     * Confirm subscription using confirmation token.
     * Updates contact status to SUBSCRIBED.
     *
     * GET /api/v1/public/confirm/{token}
     *
     * @param token the confirmation token
     * @return confirmation response (HTML page or JSON)
     */
    @GetMapping("/confirm/{token}")
    public ResponseEntity<String> confirmSubscription(@PathVariable String token) {
        log.info("Subscription confirmation request for token: {}", token);

        try {
            Contact contact = subscriptionService.confirmSubscription(token);

            // Return a simple HTML page confirming the subscription
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Subscription Confirmed</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background-color: #f5f5f5;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 8px;
                            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                            text-align: center;
                            max-width: 500px;
                        }
                        .success-icon {
                            font-size: 64px;
                            color: #28a745;
                            margin-bottom: 20px;
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 10px;
                        }
                        p {
                            color: #666;
                            line-height: 1.6;
                        }
                        .email {
                            font-weight: bold;
                            color: #007bff;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="success-icon">✓</div>
                        <h1>Subscription Confirmed!</h1>
                        <p>Thank you for confirming your subscription.</p>
                        <p>Your email address <span class="email">%s</span> has been successfully subscribed.</p>
                        <p>You can close this window now.</p>
                    </div>
                </body>
                </html>
                """, contact.getEmail());

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);

        } catch (ResourceNotFoundException e) {
            log.warn("Invalid confirmation token: {}", token);
            String html = buildErrorPage("Invalid Confirmation Link",
                    "This confirmation link is invalid or has expired.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);

        } catch (Exception e) {
            log.error("Error confirming subscription: {}", e.getMessage(), e);
            String html = buildErrorPage("Confirmation Failed",
                    "An error occurred while confirming your subscription. Please try again or contact support.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }

    /**
     * Unsubscribe using unsubscribe token.
     *
     * GET /api/v1/public/unsubscribe/{token}
     *
     * @param token the unsubscribe token
     * @param request optional unsubscribe request (reason, listId)
     * @return unsubscribe confirmation (HTML page)
     */
    @GetMapping("/unsubscribe/{token}")
    public ResponseEntity<String> unsubscribe(
            @PathVariable String token,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String listId) {

        log.info("Unsubscribe request for token: {}", token);

        try {
            Contact contact;

            if (listId != null && !listId.isEmpty()) {
                // Unsubscribe from specific list
                contact = unsubscribeService.unsubscribeFromList(token, listId);
            } else {
                // Unsubscribe from all lists
                contact = unsubscribeService.unsubscribe(token, reason);
            }

            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Unsubscribed</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background-color: #f5f5f5;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 8px;
                            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                            text-align: center;
                            max-width: 500px;
                        }
                        .info-icon {
                            font-size: 64px;
                            color: #ffc107;
                            margin-bottom: 20px;
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 10px;
                        }
                        p {
                            color: #666;
                            line-height: 1.6;
                        }
                        .email {
                            font-weight: bold;
                        }
                        .button {
                            display: inline-block;
                            margin-top: 20px;
                            padding: 12px 24px;
                            background-color: #007bff;
                            color: white;
                            text-decoration: none;
                            border-radius: 4px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="info-icon">✓</div>
                        <h1>You've Been Unsubscribed</h1>
                        <p>Your email address <span class="email">%s</span> has been unsubscribed.</p>
                        <p>You will no longer receive emails from us.</p>
                        <p>If you unsubscribed by mistake, you can manage your preferences below.</p>
                        <a href="/api/v1/public/preferences/%s" class="button">Manage Preferences</a>
                    </div>
                </body>
                </html>
                """, contact.getEmail(), token);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);

        } catch (ResourceNotFoundException e) {
            log.warn("Invalid unsubscribe token: {}", token);
            String html = buildErrorPage("Invalid Unsubscribe Link",
                    "This unsubscribe link is invalid or has expired.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);

        } catch (Exception e) {
            log.error("Error unsubscribing: {}", e.getMessage(), e);
            String html = buildErrorPage("Unsubscribe Failed",
                    "An error occurred while processing your request. Please try again or contact support.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }

    /**
     * Get subscriber preferences.
     *
     * GET /api/v1/public/preferences/{token}
     *
     * @param token the unsubscribe token
     * @return preferences data
     */
    @GetMapping("/preferences/{token}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreferences(@PathVariable String token) {
        log.info("Get preferences request for token: {}", token);

        try {
            Map<String, Object> preferences = preferenceCenterService.getPreferences(token);
            return ResponseEntity.ok(ApiResponse.success(preferences));

        } catch (ResourceNotFoundException e) {
            log.warn("Invalid preferences token: {}", token);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Invalid or expired token"));

        } catch (Exception e) {
            log.error("Error getting preferences: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve preferences"));
        }
    }

    /**
     * Update subscriber preferences.
     *
     * PUT /api/v1/public/preferences/{token}
     *
     * @param token the unsubscribe token
     * @param request the preferences update request
     * @return updated contact
     */
    @PutMapping("/preferences/{token}")
    public ResponseEntity<ApiResponse<String>> updatePreferences(
            @PathVariable String token,
            @Valid @RequestBody PreferencesUpdateRequest request) {

        log.info("Update preferences request for token: {}", token);

        try {
            // Update contact info
            if (request.getFirstName() != null || request.getLastName() != null || request.getCustomFields() != null) {
                preferenceCenterService.updateContactInfo(
                        token,
                        request.getFirstName(),
                        request.getLastName(),
                        request.getCustomFields()
                );
            }

            // Update list subscriptions
            if (request.getListIds() != null) {
                preferenceCenterService.updateListSubscriptions(token, request.getListIds());
            }

            // Update email frequency
            if (request.getEmailFrequency() != null) {
                preferenceCenterService.setEmailFrequency(token, request.getEmailFrequency());
            }

            // Update topic preferences
            if (request.getTopics() != null) {
                preferenceCenterService.setTopicPreferences(token, request.getTopics());
            }

            return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully"));

        } catch (ResourceNotFoundException e) {
            log.warn("Invalid preferences token: {}", token);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Invalid or expired token"));

        } catch (Exception e) {
            log.error("Error updating preferences: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update preferences"));
        }
    }

    /**
     * Resubscribe a previously unsubscribed contact.
     *
     * POST /api/v1/public/resubscribe/{token}
     *
     * @param token the unsubscribe token
     * @return resubscribe response
     */
    @PostMapping("/resubscribe/{token}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> resubscribe(@PathVariable String token) {
        log.info("Resubscribe request for token: {}", token);

        try {
            Contact contact = unsubscribeService.resubscribe(token);

            SubscriptionResponse response = SubscriptionResponse.success(
                    contact.getEmail(),
                    contact.getStatus(),
                    "You have been resubscribed successfully!"
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (ResourceNotFoundException e) {
            log.warn("Invalid resubscribe token: {}", token);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Invalid or expired token"));

        } catch (Exception e) {
            log.error("Error resubscribing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to resubscribe"));
        }
    }

    /**
     * Get client IP address from HTTP request.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Build an error HTML page.
     *
     * @param title the error title
     * @param message the error message
     * @return HTML string
     */
    private String buildErrorPage(String title, String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                        background-color: #f5f5f5;
                    }
                    .container {
                        background: white;
                        padding: 40px;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        text-align: center;
                        max-width: 500px;
                    }
                    .error-icon {
                        font-size: 64px;
                        color: #dc3545;
                        margin-bottom: 20px;
                    }
                    h1 {
                        color: #333;
                        margin-bottom: 10px;
                    }
                    p {
                        color: #666;
                        line-height: 1.6;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="error-icon">✗</div>
                    <h1>%s</h1>
                    <p>%s</p>
                </div>
            </body>
            </html>
            """, title, title, message);
    }
}
