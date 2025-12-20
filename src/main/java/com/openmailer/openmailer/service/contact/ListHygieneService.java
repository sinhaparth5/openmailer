package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for maintaining list hygiene.
 * Performs scheduled cleanup of bounced contacts, flags inactive contacts,
 * and removes old hard bounces.
 */
@Service
@Transactional
public class ListHygieneService {

    private static final Logger log = LoggerFactory.getLogger(ListHygieneService.class);

    private static final int INACTIVE_THRESHOLD_MONTHS = 6;
    private static final int HARD_BOUNCE_REMOVAL_DAYS = 30;

    private final ContactRepository contactRepository;

    @Autowired
    public ListHygieneService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    /**
     * Scheduled task to clean lists daily at 2 AM.
     * Runs every day at 2:00 AM to perform list hygiene operations.
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    public void performDailyListCleanup() {
        log.info("Starting daily list hygiene cleanup");

        try {
            // Flag inactive contacts
            int inactiveCount = flagInactiveContacts();
            log.info("Flagged {} inactive contacts", inactiveCount);

            // Remove old hard bounces
            int removedCount = removeOldHardBounces();
            log.info("Removed {} old hard bounces", removedCount);

            log.info("Daily list hygiene cleanup completed successfully");

        } catch (Exception e) {
            log.error("Error during daily list cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Flags contacts as inactive if they haven't opened any emails in 6 months.
     * This doesn't remove them but marks them for review.
     *
     * @return number of contacts flagged as inactive
     */
    public int flagInactiveContacts() {
        log.info("Flagging inactive contacts (no activity in {} months)", INACTIVE_THRESHOLD_MONTHS);

        LocalDateTime inactiveThreshold = LocalDateTime.now().minusMonths(INACTIVE_THRESHOLD_MONTHS);
        int count = 0;

        // This is a simplified implementation
        // In a real system, you'd query based on last_opened_at or similar field
        // For now, we'll just log the intent
        log.debug("Would flag contacts with no opens since {}", inactiveThreshold);

        // TODO: Implement based on campaign recipient tracking
        // Query: SELECT contacts WHERE last_activity < inactiveThreshold AND status = 'SUBSCRIBED'

        return count;
    }

    /**
     * Removes contacts that have been in BOUNCED status for more than 30 days.
     * This helps keep the database clean and comply with best practices.
     *
     * @return number of contacts removed
     */
    public int removeOldHardBounces() {
        log.info("Removing hard bounces older than {} days", HARD_BOUNCE_REMOVAL_DAYS);

        LocalDateTime removalThreshold = LocalDateTime.now().minusDays(HARD_BOUNCE_REMOVAL_DAYS);
        int count = 0;

        // Find bounced contacts older than threshold
        List<Contact> allContacts = contactRepository.findAll();

        for (Contact contact : allContacts) {
            if ("BOUNCED".equals(contact.getStatus()) &&
                contact.getLastBouncedAt() != null &&
                contact.getLastBouncedAt().isBefore(removalThreshold)) {

                // Option 1: Permanently delete (use with caution)
                // contactRepository.delete(contact);

                // Option 2: Mark as archived (safer approach)
                contact.setStatus("ARCHIVED");
                contactRepository.save(contact);

                count++;
            }
        }

        log.info("Archived {} old hard bounced contacts", count);
        return count;
    }

    /**
     * Cleans up duplicate contacts based on email address.
     * Keeps the most recently created contact and removes older duplicates.
     *
     * @param userId the user ID to clean duplicates for
     * @return number of duplicates removed
     */
    public int removeDuplicateContacts(String userId) {
        log.info("Removing duplicate contacts for user: {}", userId);

        int removedCount = 0;

        // This is a simplified implementation
        // In production, you'd use a more efficient query

        List<Contact> contacts = contactRepository.findByUser_Id(userId);

        // Group by email (case-insensitive)
        var emailGroups = new java.util.HashMap<String, java.util.List<Contact>>();

        for (Contact contact : contacts) {
            String emailLower = contact.getEmail().toLowerCase();
            emailGroups.computeIfAbsent(emailLower, k -> new java.util.ArrayList<>()).add(contact);
        }

        // For each group with duplicates, keep the newest and remove others
        for (var entry : emailGroups.entrySet()) {
            List<Contact> duplicates = entry.getValue();

            if (duplicates.size() > 1) {
                // Sort by created date (newest first)
                duplicates.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

                // Keep the first (newest), remove the rest
                for (int i = 1; i < duplicates.size(); i++) {
                    Contact duplicate = duplicates.get(i);
                    log.debug("Removing duplicate contact: {} (older duplicate of {})",
                            duplicate.getId(), duplicates.get(0).getId());

                    contactRepository.delete(duplicate);
                    removedCount++;
                }
            }
        }

        log.info("Removed {} duplicate contacts for user {}", removedCount, userId);
        return removedCount;
    }

    /**
     * Validates and cleans up invalid email addresses.
     * Uses commons-validator to check email format.
     *
     * @param userId the user ID to validate contacts for
     * @return number of invalid contacts found
     */
    public int validateAndFlagInvalidEmails(String userId) {
        log.info("Validating email addresses for user: {}", userId);

        int invalidCount = 0;
        List<Contact> contacts = contactRepository.findByUser_Id(userId);

        org.apache.commons.validator.routines.EmailValidator validator =
                org.apache.commons.validator.routines.EmailValidator.getInstance();

        for (Contact contact : contacts) {
            if (!validator.isValid(contact.getEmail())) {
                log.warn("Invalid email format detected: {}", contact.getEmail());

                // Mark as bounced or create a custom status
                if (!"BOUNCED".equals(contact.getStatus())) {
                    contact.setStatus("INVALID");
                    String notes = contact.getNotes() != null ? contact.getNotes() : "";
                    notes += "\n[" + LocalDateTime.now() + "] Invalid email format detected";
                    contact.setNotes(notes.trim());

                    contactRepository.save(contact);
                    invalidCount++;
                }
            }
        }

        log.info("Found and flagged {} invalid email addresses", invalidCount);
        return invalidCount;
    }

    /**
     * Reactivates contacts that were previously unsubscribed but have re-subscribed.
     * This should be called when processing subscription confirmations.
     *
     * @param contactId the contact ID to reactivate
     * @return the reactivated contact
     */
    public Contact reactivateContact(String contactId) {
        log.info("Reactivating contact: {}", contactId);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found: " + contactId));

        contact.setStatus("SUBSCRIBED");
        contact.setUnsubscribedAt(null);
        contact.setBounceCount(0);
        contact.setLastBouncedAt(null);

        String notes = contact.getNotes() != null ? contact.getNotes() : "";
        notes += "\n[" + LocalDateTime.now() + "] Contact reactivated";
        contact.setNotes(notes.trim());

        Contact updated = contactRepository.save(contact);

        log.info("Contact reactivated successfully: {}", contact.getEmail());
        return updated;
    }

    /**
     * Gets list hygiene statistics for a user.
     *
     * @param userId the user ID
     * @return hygiene statistics
     */
    public HygieneStatistics getHygieneStatistics(String userId) {
        List<Contact> contacts = contactRepository.findByUser_Id(userId);

        long totalContacts = contacts.size();
        long subscribedCount = contacts.stream()
                .filter(c -> "SUBSCRIBED".equals(c.getStatus()))
                .count();
        long bouncedCount = contacts.stream()
                .filter(c -> "BOUNCED".equals(c.getStatus()))
                .count();
        long unsubscribedCount = contacts.stream()
                .filter(c -> "UNSUBSCRIBED".equals(c.getStatus()))
                .count();

        // Count contacts with high bounce rates
        long highBounceCount = contacts.stream()
                .filter(c -> c.getBounceCount() != null && c.getBounceCount() >= 2)
                .count();

        HygieneStatistics stats = new HygieneStatistics();
        stats.setTotalContacts(totalContacts);
        stats.setSubscribedCount(subscribedCount);
        stats.setBouncedCount(bouncedCount);
        stats.setUnsubscribedCount(unsubscribedCount);
        stats.setHighBounceCount(highBounceCount);

        if (totalContacts > 0) {
            stats.setHealthScore((double) subscribedCount / totalContacts * 100);
        }

        return stats;
    }

    /**
     * Manually triggers the list cleanup process.
     * Useful for testing or manual execution.
     */
    public void triggerManualCleanup() {
        log.info("Manually triggering list hygiene cleanup");
        performDailyListCleanup();
    }

    /**
     * Data class for hygiene statistics.
     */
    public static class HygieneStatistics {
        private long totalContacts;
        private long subscribedCount;
        private long bouncedCount;
        private long unsubscribedCount;
        private long highBounceCount;
        private double healthScore;

        public long getTotalContacts() {
            return totalContacts;
        }

        public void setTotalContacts(long totalContacts) {
            this.totalContacts = totalContacts;
        }

        public long getSubscribedCount() {
            return subscribedCount;
        }

        public void setSubscribedCount(long subscribedCount) {
            this.subscribedCount = subscribedCount;
        }

        public long getBouncedCount() {
            return bouncedCount;
        }

        public void setBouncedCount(long bouncedCount) {
            this.bouncedCount = bouncedCount;
        }

        public long getUnsubscribedCount() {
            return unsubscribedCount;
        }

        public void setUnsubscribedCount(long unsubscribedCount) {
            this.unsubscribedCount = unsubscribedCount;
        }

        public long getHighBounceCount() {
            return highBounceCount;
        }

        public void setHighBounceCount(long highBounceCount) {
            this.highBounceCount = highBounceCount;
        }

        public double getHealthScore() {
            return healthScore;
        }

        public void setHealthScore(double healthScore) {
            this.healthScore = healthScore;
        }
    }
}
