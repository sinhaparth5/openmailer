package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactListMembership;
import com.openmailer.openmailer.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling unsubscribe requests.
 * Manages contact opt-out and preference management.
 */
@Service
@Transactional
public class UnsubscribeService {

    private static final Logger log = LoggerFactory.getLogger(UnsubscribeService.class);

    private final ContactRepository contactRepository;
    private final ContactListMembershipService membershipService;

    @Autowired
    public UnsubscribeService(
            ContactRepository contactRepository,
            ContactListMembershipService membershipService) {
        this.contactRepository = contactRepository;
        this.membershipService = membershipService;
    }

    /**
     * Unsubscribe a contact using their unsubscribe token.
     * Updates the contact status to UNSUBSCRIBED and removes from all lists.
     *
     * @param token the unsubscribe token
     * @param reason the reason for unsubscribing (optional)
     * @return the unsubscribed contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact unsubscribe(String token, String reason) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Unsubscribe token", "token", token));

        // Check if already unsubscribed
        if ("UNSUBSCRIBED".equals(contact.getStatus())) {
            log.info("Contact {} already unsubscribed", contact.getEmail());
            return contact;
        }

        // Update contact status
        contact.setStatus("UNSUBSCRIBED");
        contact.setUnsubscribedAt(LocalDateTime.now());
        contact.setUnsubscribeReason(reason);

        contact = contactRepository.save(contact);

        // Remove from all lists
        removeFromAllLists(contact.getId());

        log.info("Contact {} unsubscribed. Reason: {}", contact.getEmail(), reason);
        return contact;
    }

    /**
     * Unsubscribe a contact from a specific list only.
     * The contact remains subscribed to other lists.
     *
     * @param token the unsubscribe token
     * @param listId the list ID to unsubscribe from
     * @return the contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact unsubscribeFromList(String token, String listId) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Unsubscribe token", "token", token));

        // Remove from the specific list
        try {
            membershipService.removeContactFromList(contact.getId(), listId);
            log.info("Contact {} unsubscribed from list {}", contact.getEmail(), listId);
        } catch (Exception e) {
            log.warn("Failed to remove contact {} from list {}: {}",
                    contact.getEmail(), listId, e.getMessage());
        }

        // Check if contact is still in any lists
        List<ContactListMembership> memberships = membershipService.findByContact(contact.getId());

        // If not in any lists, mark as unsubscribed
        if (memberships.isEmpty()) {
            contact.setStatus("UNSUBSCRIBED");
            contact.setUnsubscribedAt(LocalDateTime.now());
            contact.setUnsubscribeReason("Unsubscribed from all lists");
            contact = contactRepository.save(contact);
            log.info("Contact {} unsubscribed from all lists", contact.getEmail());
        }

        return contact;
    }

    /**
     * Resubscribe a previously unsubscribed contact.
     * This should only be used when the contact explicitly requests to resubscribe.
     *
     * @param token the unsubscribe token (used to identify the contact)
     * @return the resubscribed contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact resubscribe(String token) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Unsubscribe token", "token", token));

        // Only allow resubscribe if currently unsubscribed
        if (!"UNSUBSCRIBED".equals(contact.getStatus())) {
            log.info("Contact {} is not unsubscribed, cannot resubscribe", contact.getEmail());
            return contact;
        }

        // Update contact status back to subscribed
        contact.setStatus("SUBSCRIBED");
        contact.setSubscribedAt(LocalDateTime.now());
        contact.setUnsubscribedAt(null);
        contact.setUnsubscribeReason(null);

        contact = contactRepository.save(contact);
        log.info("Contact {} resubscribed", contact.getEmail());

        return contact;
    }

    /**
     * Get unsubscribe details for a contact by token.
     *
     * @param token the unsubscribe token
     * @return the contact
     * @throws ResourceNotFoundException if token is invalid
     */
    @Transactional(readOnly = true)
    public Contact getContactByUnsubscribeToken(String token) {
        return contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Unsubscribe token", "token", token));
    }

    /**
     * Remove contact from all lists.
     *
     * @param contactId the contact ID
     */
    private void removeFromAllLists(String contactId) {
        try {
            List<ContactListMembership> memberships = membershipService.findByContact(contactId);

            for (ContactListMembership membership : memberships) {
                membershipService.removeContactFromList(contactId, membership.getListId());
            }

            log.info("Removed contact {} from {} lists", contactId, memberships.size());
        } catch (Exception e) {
            log.error("Failed to remove contact {} from lists: {}", contactId, e.getMessage(), e);
        }
    }

    /**
     * Check if a contact is unsubscribed.
     *
     * @param email the email address
     * @param userId the user ID (list owner)
     * @return true if unsubscribed, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUnsubscribed(String email, String userId) {
        return contactRepository.findByEmailAndUser_Id(email, userId)
                .map(contact -> "UNSUBSCRIBED".equals(contact.getStatus()))
                .orElse(false);
    }
}
