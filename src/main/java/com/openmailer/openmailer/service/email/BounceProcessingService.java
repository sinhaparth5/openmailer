package com.openmailer.openmailer.service.email;

import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for processing email bounces.
 * Handles hard bounces (immediate unsubscribe) and soft bounces (3 strikes).
 */
@Service
@Transactional
public class BounceProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BounceProcessingService.class);

    private static final int MAX_SOFT_BOUNCES = 3;
    private static final String STATUS_BOUNCED = "BOUNCED";
    private static final String STATUS_UNSUBSCRIBED = "UNSUBSCRIBED";

    private final ContactRepository contactRepository;

    @Autowired
    public BounceProcessingService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    /**
     * Processes a hard bounce.
     * Hard bounces are permanent failures (invalid email, domain doesn't exist, etc.)
     * Immediately marks the contact as BOUNCED.
     *
     * @param contactId the contact ID
     * @param bounceReason the reason for the bounce
     * @return the updated contact
     */
    public Contact processHardBounce(String contactId, String bounceReason) {
        log.info("Processing hard bounce for contact: {}", contactId);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found: " + contactId));

        // Increment bounce count
        int currentBounces = contact.getBounceCount() != null ? contact.getBounceCount() : 0;
        contact.setBounceCount(currentBounces + 1);

        // Mark as bounced immediately
        contact.setStatus(STATUS_BOUNCED);
        contact.setLastBouncedAt(LocalDateTime.now());

        // Store bounce reason in notes or custom field
        String notes = contact.getNotes() != null ? contact.getNotes() : "";
        notes += "\n[" + LocalDateTime.now() + "] Hard bounce: " + bounceReason;
        contact.setNotes(notes.trim());

        Contact updated = contactRepository.save(contact);

        log.warn("Contact {} marked as BOUNCED due to hard bounce: {}",
                contact.getEmail(), bounceReason);

        return updated;
    }

    /**
     * Processes a soft bounce.
     * Soft bounces are temporary failures (mailbox full, server temporarily unavailable, etc.)
     * Marks contact as BOUNCED after 3 soft bounces.
     *
     * @param contactId the contact ID
     * @param bounceReason the reason for the bounce
     * @return the updated contact
     */
    public Contact processSoftBounce(String contactId, String bounceReason) {
        log.info("Processing soft bounce for contact: {}", contactId);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found: " + contactId));

        // Increment bounce count
        int currentBounces = contact.getBounceCount() != null ? contact.getBounceCount() : 0;
        contact.setBounceCount(currentBounces + 1);
        contact.setLastBouncedAt(LocalDateTime.now());

        // Store bounce reason
        String notes = contact.getNotes() != null ? contact.getNotes() : "";
        notes += "\n[" + LocalDateTime.now() + "] Soft bounce (" + contact.getBounceCount() + "/" + MAX_SOFT_BOUNCES + "): " + bounceReason;
        contact.setNotes(notes.trim());

        // Check if we've reached the threshold
        if (contact.getBounceCount() >= MAX_SOFT_BOUNCES) {
            contact.setStatus(STATUS_BOUNCED);
            log.warn("Contact {} marked as BOUNCED after {} soft bounces",
                    contact.getEmail(), contact.getBounceCount());
        } else {
            log.info("Contact {} soft bounce recorded ({}/{})",
                    contact.getEmail(), contact.getBounceCount(), MAX_SOFT_BOUNCES);
        }

        return contactRepository.save(contact);
    }

    /**
     * Processes a complaint (spam report).
     * Immediately unsubscribes the contact and marks them as complained.
     *
     * @param contactId the contact ID
     * @param complaintReason the reason for the complaint
     * @return the updated contact
     */
    public Contact processComplaint(String contactId, String complaintReason) {
        log.info("Processing spam complaint for contact: {}", contactId);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found: " + contactId));

        // Mark as unsubscribed immediately
        contact.setStatus(STATUS_UNSUBSCRIBED);
        contact.setUnsubscribedAt(LocalDateTime.now());

        // Store complaint reason
        String notes = contact.getNotes() != null ? contact.getNotes() : "";
        notes += "\n[" + LocalDateTime.now() + "] Spam complaint: " + complaintReason;
        contact.setNotes(notes.trim());

        Contact updated = contactRepository.save(contact);

        log.warn("Contact {} unsubscribed due to spam complaint: {}",
                contact.getEmail(), complaintReason);

        return updated;
    }

    /**
     * Processes a bounce by email address.
     * Useful when processing webhook events where we might only have the email.
     *
     * @param email the email address
     * @param userId the user ID who owns this contact
     * @param isHardBounce whether this is a hard bounce
     * @param bounceReason the reason for the bounce
     * @return the updated contact, or null if not found
     */
    public Contact processBounceByEmail(String email, String userId, boolean isHardBounce, String bounceReason) {
        log.info("Processing {} bounce for email: {}", isHardBounce ? "hard" : "soft", email);

        return contactRepository.findByEmailAndUser_Id(email, userId)
                .map(contact -> {
                    if (isHardBounce) {
                        return processHardBounce(contact.getId(), bounceReason);
                    } else {
                        return processSoftBounce(contact.getId(), bounceReason);
                    }
                })
                .orElseGet(() -> {
                    log.warn("Contact not found for email: {}", email);
                    return null;
                });
    }

    /**
     * Processes a complaint by email address.
     *
     * @param email the email address
     * @param userId the user ID who owns this contact
     * @param complaintReason the reason for the complaint
     * @return the updated contact, or null if not found
     */
    public Contact processComplaintByEmail(String email, String userId, String complaintReason) {
        log.info("Processing complaint for email: {}", email);

        return contactRepository.findByEmailAndUser_Id(email, userId)
                .map(contact -> processComplaint(contact.getId(), complaintReason))
                .orElseGet(() -> {
                    log.warn("Contact not found for email: {}", email);
                    return null;
                });
    }

    /**
     * Resets the bounce count for a contact.
     * Useful when a contact has been verified or their email has been updated.
     *
     * @param contactId the contact ID
     * @return the updated contact
     */
    public Contact resetBounceCount(String contactId) {
        log.info("Resetting bounce count for contact: {}", contactId);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found: " + contactId));

        contact.setBounceCount(0);
        contact.setLastBouncedAt(null);

        return contactRepository.save(contact);
    }

    /**
     * Checks if a contact should be excluded from campaigns due to bounces.
     *
     * @param contact the contact to check
     * @return true if the contact should be excluded
     */
    public boolean shouldExcludeFromCampaigns(Contact contact) {
        if (contact == null) {
            return true;
        }

        String status = contact.getStatus();

        // Exclude bounced and unsubscribed contacts
        return STATUS_BOUNCED.equals(status) ||
               STATUS_UNSUBSCRIBED.equals(status) ||
               "COMPLAINED".equals(status);
    }

    /**
     * Gets bounce statistics for a user.
     *
     * @param userId the user ID
     * @return bounce statistics
     */
    public BounceStatistics getBounceStatistics(String userId) {
        long totalContacts = contactRepository.countByUser_Id(userId);
        long bouncedContacts = contactRepository.countByUser_IdAndStatus(userId, STATUS_BOUNCED);
        long hardBounces = bouncedContacts; // Simplified - could track separately
        long softBounces = 0; // Would need additional tracking

        BounceStatistics stats = new BounceStatistics();
        stats.setTotalContacts(totalContacts);
        stats.setBouncedContacts(bouncedContacts);
        stats.setHardBounces(hardBounces);
        stats.setSoftBounces(softBounces);

        if (totalContacts > 0) {
            stats.setBounceRate((double) bouncedContacts / totalContacts * 100);
        }

        return stats;
    }

    /**
     * Data class for bounce statistics.
     */
    public static class BounceStatistics {
        private long totalContacts;
        private long bouncedContacts;
        private long hardBounces;
        private long softBounces;
        private double bounceRate;

        public long getTotalContacts() {
            return totalContacts;
        }

        public void setTotalContacts(long totalContacts) {
            this.totalContacts = totalContacts;
        }

        public long getBouncedContacts() {
            return bouncedContacts;
        }

        public void setBouncedContacts(long bouncedContacts) {
            this.bouncedContacts = bouncedContacts;
        }

        public long getHardBounces() {
            return hardBounces;
        }

        public void setHardBounces(long hardBounces) {
            this.hardBounces = hardBounces;
        }

        public long getSoftBounces() {
            return softBounces;
        }

        public void setSoftBounces(long softBounces) {
            this.softBounces = softBounces;
        }

        public double getBounceRate() {
            return bounceRate;
        }

        public void setBounceRate(double bounceRate) {
            this.bounceRate = bounceRate;
        }
    }
}
