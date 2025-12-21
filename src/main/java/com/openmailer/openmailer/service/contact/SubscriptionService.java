package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.ContactListMembership;
import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.ContactRepository;
import com.openmailer.openmailer.service.email.EmailSender;
import com.openmailer.openmailer.service.email.EmailSender.EmailSendRequest;
import com.openmailer.openmailer.service.email.provider.ProviderFactory;
import com.openmailer.openmailer.service.provider.EmailProviderService;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling public subscription requests.
 * Manages the double opt-in flow: subscribe → send confirmation → confirm → subscribed.
 */
@Service
@Transactional
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final ContactRepository contactRepository;
    private final ContactListService contactListService;
    private final ContactListMembershipService membershipService;
    private final EmailProviderService providerService;
    private final ProviderFactory providerFactory;

    @Autowired
    public SubscriptionService(
            ContactRepository contactRepository,
            ContactListService contactListService,
            ContactListMembershipService membershipService,
            EmailProviderService providerService,
            ProviderFactory providerFactory) {
        this.contactRepository = contactRepository;
        this.contactListService = contactListService;
        this.membershipService = membershipService;
        this.providerService = providerService;
        this.providerFactory = providerFactory;
    }

    /**
     * Handle a public subscription request (double opt-in).
     * Creates a contact with PENDING status and sends a confirmation email.
     *
     * @param email the email address
     * @param firstName the first name (optional)
     * @param lastName the last name (optional)
     * @param listId the contact list ID to subscribe to
     * @param ipAddress the IP address for GDPR compliance
     * @param source the subscription source (e.g., "website", "landing-page")
     * @return the created contact
     * @throws ValidationException if email is invalid or list doesn't exist
     */
    @Transactional(noRollbackFor = ResourceNotFoundException.class)
    public Contact subscribe(String email, String firstName, String lastName,
                           String listId, String ipAddress, String source) {

        // Validate email format
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new ValidationException("Invalid email format", "email");
        }

        // Get the contact list to determine the user (list owner)
        ContactList list = contactListService.findById(listId);
        User user = list.getUser();

        // Check if contact already exists
        Contact contact = contactRepository.findByEmailAndUser_Id(email, user.getId())
                .orElse(null);

        if (contact != null) {
            // If already subscribed, do nothing (return existing contact)
            if ("SUBSCRIBED".equals(contact.getStatus())) {
                log.info("Contact {} already subscribed to user {}", email, user.getId());
                return contact;
            }

            // If previously unsubscribed, allow re-subscription with new token
            if ("UNSUBSCRIBED".equals(contact.getStatus())) {
                log.info("Contact {} re-subscribing after unsubscribe", email);
                contact.setStatus("PENDING");
                contact.setConfirmationToken(generateToken());
                contact.setConfirmationSentAt(LocalDateTime.now());
                contact.setUnsubscribedAt(null);
                contact.setUnsubscribeReason(null);
                contact.setSource(source);
            } else {
                // Resend confirmation for pending contacts
                log.info("Resending confirmation to contact {}", email);
                contact.setConfirmationToken(generateToken());
                contact.setConfirmationSentAt(LocalDateTime.now());
            }
        } else {
            // Create new contact
            contact = new Contact(user, email);
            contact.setFirstName(firstName);
            contact.setLastName(lastName);
            contact.setStatus("PENDING");
            contact.setConfirmationToken(generateToken());
            contact.setConfirmationSentAt(LocalDateTime.now());
            contact.setSource(source);
            contact.setGdprConsent(true);
            contact.setGdprConsentDate(LocalDateTime.now());
            contact.setGdprIpAddress(ipAddress);
            contact.setUnsubscribeToken(generateToken());
        }

        // Save contact
        contact = contactRepository.save(contact);

        // Add to list (if not already a member)
        try {
            if (!membershipService.isContactInList(contact.getId(), listId)) {
                ContactListMembership membership = new ContactListMembership();
                membership.setContactId(contact.getId());
                membership.setListId(listId);
                membership.setStatus("ACTIVE");
                membershipService.addContactToList(membership);
            }
        } catch (Exception e) {
            log.debug("Contact already in list or error adding to list: {}", e.getMessage());
        }

        // Send confirmation email
        sendConfirmationEmail(contact, list);

        log.info("Subscription request created for {} in list {}", email, listId);
        return contact;
    }

    /**
     * Confirm a subscription using the confirmation token.
     * Updates the contact status to SUBSCRIBED.
     *
     * @param token the confirmation token
     * @return the confirmed contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact confirmSubscription(String token) {
        Contact contact = contactRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Confirmation token", "token", token));

        // Check if already confirmed
        if ("SUBSCRIBED".equals(contact.getStatus())) {
            log.info("Contact {} already confirmed", contact.getEmail());
            return contact;
        }

        // Update contact status
        contact.setStatus("SUBSCRIBED");
        contact.setEmailVerified(true);
        contact.setConfirmedAt(LocalDateTime.now());
        contact.setSubscribedAt(LocalDateTime.now());

        contact = contactRepository.save(contact);
        log.info("Subscription confirmed for {}", contact.getEmail());

        return contact;
    }

    /**
     * Send confirmation email to a contact.
     *
     * @param contact the contact
     * @param list the contact list
     */
    private void sendConfirmationEmail(Contact contact, ContactList list) {
        try {
            // Get the user's default email provider
            EmailProvider provider = providerService.findDefaultProvider(contact.getUserId());

            if (provider == null) {
                log.warn("No default email provider found for user {}. Skipping confirmation email.",
                        contact.getUserId());
                return;
            }

            // Parse configuration to get from email and name
            Map<String, String> config = provider.getConfigurationMap();
            String fromEmail = config.getOrDefault("fromEmail", "noreply@example.com");
            String fromName = config.getOrDefault("fromName", list.getName());

            // Generate confirmation URL
            String confirmationUrl = baseUrl + "/api/v1/public/confirm/" + contact.getConfirmationToken();

            // Build email content
            String subject = "Please confirm your subscription";
            String htmlBody = buildConfirmationEmailHtml(contact, confirmationUrl, list.getName());
            String textBody = buildConfirmationEmailText(contact, confirmationUrl, list.getName());

            // Create email request
            EmailSendRequest request = new EmailSendRequest();
            request.setFrom(fromEmail);
            request.setFromName(fromName);
            request.setTo(contact.getEmail());
            request.setSubject(subject);
            request.setHtmlBody(htmlBody);
            request.setTextBody(textBody);

            // Send email
            EmailSender sender = providerFactory.createProvider(provider);
            sender.send(request);

            log.info("Confirmation email sent to {}", contact.getEmail());

        } catch (Exception e) {
            log.error("Failed to send confirmation email to {}: {}",
                    contact.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Build HTML version of confirmation email.
     */
    private String buildConfirmationEmailHtml(Contact contact, String confirmationUrl, String listName) {
        String name = contact.getFirstName() != null ? contact.getFirstName() : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button {
                        display: inline-block;
                        padding: 12px 24px;
                        background-color: #007bff;
                        color: #ffffff;
                        text-decoration: none;
                        border-radius: 4px;
                        margin: 20px 0;
                    }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Confirm Your Subscription</h2>
                    <p>Hello %s,</p>
                    <p>Thank you for subscribing to <strong>%s</strong>.</p>
                    <p>Please click the button below to confirm your email address and complete your subscription:</p>
                    <p>
                        <a href="%s" class="button">Confirm Subscription</a>
                    </p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <div class="footer">
                        <p>If you didn't request this subscription, you can safely ignore this email.</p>
                        <p>This confirmation link will expire in 7 days.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, listName, confirmationUrl, confirmationUrl, confirmationUrl);
    }

    /**
     * Build plain text version of confirmation email.
     */
    private String buildConfirmationEmailText(Contact contact, String confirmationUrl, String listName) {
        String name = contact.getFirstName() != null ? contact.getFirstName() : "";

        return String.format("""
            Hello %s,

            Thank you for subscribing to %s.

            Please click the link below to confirm your email address and complete your subscription:

            %s

            If you didn't request this subscription, you can safely ignore this email.
            This confirmation link will expire in 7 days.
            """, name, listName, confirmationUrl);
    }

    /**
     * Generate a unique token for confirmation or unsubscribe.
     *
     * @return a random UUID token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Get subscription status for a contact by email and list.
     *
     * @param email the email address
     * @param listId the list ID
     * @return the contact if found, null otherwise
     */
    @Transactional(readOnly = true)
    public Contact getSubscriptionStatus(String email, String listId) {
        ContactList list = contactListService.findById(listId);
        return contactRepository.findByEmailAndUser_Id(email, list.getUser().getId())
                .orElse(null);
    }
}
