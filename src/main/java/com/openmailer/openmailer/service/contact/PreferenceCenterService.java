package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.ContactListMembership;
import com.openmailer.openmailer.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing subscriber preferences.
 * Allows contacts to manage their subscription settings and list memberships.
 */
@Service
@Transactional
public class PreferenceCenterService {

    private static final Logger log = LoggerFactory.getLogger(PreferenceCenterService.class);

    private final ContactRepository contactRepository;
    private final ContactListService contactListService;
    private final ContactListMembershipService membershipService;

    @Autowired
    public PreferenceCenterService(
            ContactRepository contactRepository,
            ContactListService contactListService,
            ContactListMembershipService membershipService) {
        this.contactRepository = contactRepository;
        this.contactListService = contactListService;
        this.membershipService = membershipService;
    }

    /**
     * Get contact preferences by unsubscribe token.
     *
     * @param token the unsubscribe token
     * @return a map containing contact details and preferences
     * @throws ResourceNotFoundException if token is invalid
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPreferences(String token) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "token", token));

        Map<String, Object> preferences = new HashMap<>();

        // Contact details
        preferences.put("email", contact.getEmail());
        preferences.put("firstName", contact.getFirstName());
        preferences.put("lastName", contact.getLastName());
        preferences.put("status", contact.getStatus());

        // Get all lists for this user
        List<ContactList> allLists = contactListService.findByUserId(contact.getUserId());

        // Get contact's current list memberships
        List<ContactListMembership> memberships = membershipService.findByContact(contact.getId());
        List<String> subscribedListIds = memberships.stream()
                .map(ContactListMembership::getListId)
                .collect(Collectors.toList());

        // Build list information
        List<Map<String, Object>> lists = allLists.stream()
                .map(list -> {
                    Map<String, Object> listInfo = new HashMap<>();
                    listInfo.put("id", list.getId());
                    listInfo.put("name", list.getName());
                    listInfo.put("description", list.getDescription());
                    listInfo.put("subscribed", subscribedListIds.contains(list.getId()));
                    return listInfo;
                })
                .collect(Collectors.toList());

        preferences.put("lists", lists);

        // Custom fields
        if (contact.getCustomFields() != null) {
            preferences.put("customFields", contact.getCustomFields());
        }

        log.info("Retrieved preferences for contact {}", contact.getEmail());
        return preferences;
    }

    /**
     * Update contact information (name, custom fields).
     *
     * @param token the unsubscribe token
     * @param firstName the first name (optional)
     * @param lastName the last name (optional)
     * @param customFields custom fields (optional)
     * @return the updated contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact updateContactInfo(String token, String firstName, String lastName,
                                    Map<String, Object> customFields) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "token", token));

        if (firstName != null) {
            contact.setFirstName(firstName);
        }

        if (lastName != null) {
            contact.setLastName(lastName);
        }

        if (customFields != null) {
            // Merge with existing custom fields
            Map<String, Object> existing = contact.getCustomFields();
            if (existing == null) {
                contact.setCustomFields(customFields);
            } else {
                existing.putAll(customFields);
                contact.setCustomFields(existing);
            }
        }

        contact = contactRepository.save(contact);
        log.info("Updated contact info for {}", contact.getEmail());

        return contact;
    }

    /**
     * Update list subscriptions for a contact.
     * Adds contact to selected lists and removes from unselected lists.
     *
     * @param token the unsubscribe token
     * @param listIds the list of list IDs to subscribe to
     * @return the updated contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact updateListSubscriptions(String token, List<String> listIds) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "token", token));

        // Get all lists for this user
        List<ContactList> allLists = contactListService.findByUserId(contact.getUserId());

        // Get current memberships
        List<ContactListMembership> currentMemberships = membershipService.findByContact(contact.getId());
        List<String> currentListIds = currentMemberships.stream()
                .map(ContactListMembership::getListId)
                .collect(Collectors.toList());

        // Add to new lists
        for (String listId : listIds) {
            if (!currentListIds.contains(listId)) {
                try {
                    ContactListMembership membership = new ContactListMembership();
                    membership.setContactId(contact.getId());
                    membership.setListId(listId);
                    membership.setStatus("ACTIVE");
                    membershipService.addContactToList(membership);
                    log.info("Added contact {} to list {}", contact.getEmail(), listId);
                } catch (Exception e) {
                    log.error("Failed to add contact to list {}: {}", listId, e.getMessage());
                }
            }
        }

        // Remove from lists not in the new selection
        for (String currentListId : currentListIds) {
            if (!listIds.contains(currentListId)) {
                try {
                    membershipService.removeContactFromList(contact.getId(), currentListId);
                    log.info("Removed contact {} from list {}", contact.getEmail(), currentListId);
                } catch (Exception e) {
                    log.error("Failed to remove contact from list {}: {}", currentListId, e.getMessage());
                }
            }
        }

        // Update contact status based on list subscriptions
        if (listIds.isEmpty()) {
            // No lists selected, mark as unsubscribed
            contact.setStatus("UNSUBSCRIBED");
        } else if ("UNSUBSCRIBED".equals(contact.getStatus())) {
            // Had unsubscribed but now selecting lists again, mark as subscribed
            contact.setStatus("SUBSCRIBED");
        }

        contact = contactRepository.save(contact);
        log.info("Updated list subscriptions for contact {}", contact.getEmail());

        return contact;
    }

    /**
     * Set email frequency preference.
     * This is stored in the contact's custom fields.
     *
     * @param token the unsubscribe token
     * @param frequency the frequency preference (e.g., "daily", "weekly", "monthly")
     * @return the updated contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact setEmailFrequency(String token, String frequency) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "token", token));

        Map<String, Object> customFields = contact.getCustomFields();
        if (customFields == null) {
            customFields = new HashMap<>();
        }

        customFields.put("emailFrequency", frequency);
        contact.setCustomFields(customFields);

        contact = contactRepository.save(contact);
        log.info("Set email frequency to {} for contact {}", frequency, contact.getEmail());

        return contact;
    }

    /**
     * Set topic preferences.
     * This is stored in the contact's custom fields as a list of topics.
     *
     * @param token the unsubscribe token
     * @param topics the list of topics the contact is interested in
     * @return the updated contact
     * @throws ResourceNotFoundException if token is invalid
     */
    public Contact setTopicPreferences(String token, List<String> topics) {
        Contact contact = contactRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "token", token));

        Map<String, Object> customFields = contact.getCustomFields();
        if (customFields == null) {
            customFields = new HashMap<>();
        }

        customFields.put("topicPreferences", topics);
        contact.setCustomFields(customFields);

        contact = contactRepository.save(contact);
        log.info("Updated topic preferences for contact {}", contact.getEmail());

        return contact;
    }
}
