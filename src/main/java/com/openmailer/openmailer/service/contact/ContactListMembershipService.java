package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.ContactListMembership;
import com.openmailer.openmailer.repository.ContactListMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for ContactListMembership management operations.
 * Handles adding/removing contacts to/from lists.
 */
@Service
@Transactional
public class ContactListMembershipService {

  private final ContactListMembershipRepository membershipRepository;

  @Autowired
  public ContactListMembershipService(ContactListMembershipRepository membershipRepository) {
    this.membershipRepository = membershipRepository;
  }

  /**
   * Add contact to list.
   *
   * @param membership the membership to create
   * @return the created membership
   * @throws ValidationException if contact is already in the list
   */
  public ContactListMembership addContactToList(ContactListMembership membership) {
    // Check if membership already exists
    if (membershipRepository.existsByContactIdAndListId(membership.getContactId(), membership.getListId())) {
      throw new ValidationException("Contact is already in this list", "membership");
    }

    // Set default status if not provided
    if (membership.getStatus() == null) {
      membership.setStatus("ACTIVE");
    }

    membership.setAddedAt(LocalDateTime.now());
    return membershipRepository.save(membership);
  }

  /**
   * Add multiple contacts to a list.
   *
   * @param contactIds list of contact IDs
   * @param listId the list ID
   * @return list of created memberships
   */
  public List<ContactListMembership> addContactsToList(List<String> contactIds, String listId) {
    return contactIds.stream()
        .filter(contactId -> !membershipRepository.existsByContactIdAndListId(contactId, listId))
        .map(contactId -> {
          ContactListMembership membership = new ContactListMembership();
          membership.setContactId(contactId);
          membership.setListId(listId);
          membership.setStatus("ACTIVE");
          membership.setAddedAt(LocalDateTime.now());
          return membershipRepository.save(membership);
        })
        .toList();
  }

  /**
   * Find membership by contact and list.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   * @return the membership
   * @throws ResourceNotFoundException if membership not found
   */
  @Transactional(readOnly = true)
  public ContactListMembership findByContactAndList(String contactId, String listId) {
    return membershipRepository.findByContactIdAndListId(contactId, listId)
        .orElseThrow(() -> new ResourceNotFoundException("ContactListMembership not found"));
  }

  /**
   * Find all memberships for a contact.
   *
   * @param contactId the contact ID
   * @return list of memberships
   */
  @Transactional(readOnly = true)
  public List<ContactListMembership> findByContact(String contactId) {
    return membershipRepository.findByContactId(contactId);
  }

  /**
   * Find all memberships for a list.
   *
   * @param listId the list ID
   * @return list of memberships
   */
  @Transactional(readOnly = true)
  public List<ContactListMembership> findByList(String listId) {
    return membershipRepository.findByListId(listId);
  }

  /**
   * Find all memberships for a list with pagination.
   *
   * @param listId the list ID
   * @param pageable pagination information
   * @return page of memberships
   */
  @Transactional(readOnly = true)
  public Page<ContactListMembership> findByList(String listId, Pageable pageable) {
    return membershipRepository.findByListId(listId, pageable);
  }

  /**
   * Find memberships by status.
   *
   * @param listId the list ID
   * @param status the membership status
   * @param pageable pagination information
   * @return page of memberships
   */
  @Transactional(readOnly = true)
  public Page<ContactListMembership> findByStatus(String listId, String status, Pageable pageable) {
    return membershipRepository.findByListIdAndStatus(listId, status, pageable);
  }

  /**
   * Get all contact IDs for a list.
   *
   * @param listId the list ID
   * @return list of contact IDs
   */
  @Transactional(readOnly = true)
  public List<String> getContactIdsByList(String listId) {
    return membershipRepository.findContactIdsByListId(listId);
  }

  /**
   * Get active contact IDs for a list.
   *
   * @param listId the list ID
   * @return list of contact IDs
   */
  @Transactional(readOnly = true)
  public List<String> getActiveContactIdsByList(String listId) {
    return membershipRepository.findContactIdsByListIdAndStatus(listId, "ACTIVE");
  }

  /**
   * Update membership status.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   * @param status the new status
   * @return the updated membership
   */
  public ContactListMembership updateStatus(String contactId, String listId, String status) {
    ContactListMembership membership = findByContactAndList(contactId, listId);
    membership.setStatus(status);
    return membershipRepository.save(membership);
  }

  /**
   * Remove contact from list.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   */
  public void removeContactFromList(String contactId, String listId) {
    membershipRepository.deleteByContactIdAndListId(contactId, listId);
  }

  /**
   * Remove multiple contacts from a list.
   *
   * @param contactIds list of contact IDs
   * @param listId the list ID
   */
  public void removeContactsFromList(List<String> contactIds, String listId) {
    contactIds.forEach(contactId ->
        membershipRepository.deleteByContactIdAndListId(contactId, listId)
    );
  }

  /**
   * Remove all contacts from a list.
   *
   * @param listId the list ID
   */
  public void removeAllContactsFromList(String listId) {
    membershipRepository.deleteByListId(listId);
  }

  /**
   * Count memberships for a list.
   *
   * @param listId the list ID
   * @return count of memberships
   */
  @Transactional(readOnly = true)
  public long countByList(String listId) {
    return membershipRepository.countByListId(listId);
  }

  /**
   * Count active memberships for a list.
   *
   * @param listId the list ID
   * @return count of active memberships
   */
  @Transactional(readOnly = true)
  public long countActiveByList(String listId) {
    return membershipRepository.countByListIdAndStatus(listId, "ACTIVE");
  }

  /**
   * Check if contact is in list.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   * @return true if contact is in list, false otherwise
   */
  @Transactional(readOnly = true)
  public boolean isContactInList(String contactId, String listId) {
    return membershipRepository.existsByContactIdAndListId(contactId, listId);
  }
}
