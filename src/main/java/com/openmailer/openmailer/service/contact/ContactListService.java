package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.repository.ContactListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for ContactList management operations.
 * Handles contact list CRUD operations and user authorization.
 */
@Service
@Transactional
public class ContactListService {

  private final ContactListRepository contactListRepository;

  @Autowired
  public ContactListService(ContactListRepository contactListRepository) {
    this.contactListRepository = contactListRepository;
  }

  /**
   * Create a new contact list.
   *
   * @param contactList the contact list to create
   * @return the created contact list
   * @throws ValidationException if list name already exists for user
   */
  public ContactList createContactList(ContactList contactList) {
    // Validate list name uniqueness for user
    if (contactListRepository.findByNameAndUser_Id(contactList.getName(), contactList.getUserId()).isPresent()) {
      throw new ValidationException("Contact list with this name already exists", "name");
    }

    contactList.setCreatedAt(LocalDateTime.now());
    contactList.setUpdatedAt(LocalDateTime.now());

    return contactListRepository.save(contactList);
  }

  /**
   * Find contact list by ID.
   *
   * @param id the list ID
   * @return the contact list
   * @throws ResourceNotFoundException if list not found
   */
  @Transactional(readOnly = true)
  public ContactList findById(String id) {
    return contactListRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ContactList", "id", id));
  }

  /**
   * Find contact list by ID and verify user ownership.
   *
   * @param id the list ID
   * @param userId the user ID
   * @return the contact list
   * @throws ResourceNotFoundException if list not found
   */
  @Transactional(readOnly = true)
  public ContactList findByIdAndUserId(String id, String userId) {
    return contactListRepository.findByIdAndUser_Id(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("ContactList", "id", id));
  }

  /**
   * Find all contact lists for a user.
   *
   * @param userId the user ID
   * @return list of contact lists
   */
  @Transactional(readOnly = true)
  public List<ContactList> findByUserId(String userId) {
    return contactListRepository.findByUser_Id(userId);
  }

  /**
   * Find all contact lists for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of contact lists
   */
  @Transactional(readOnly = true)
  public Page<ContactList> findByUserId(String userId, Pageable pageable) {
    return contactListRepository.findByUser_Id(userId, pageable);
  }

  /**
   * Search contact lists by name.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching contact lists
   */
  @Transactional(readOnly = true)
  public Page<ContactList> searchByName(String userId, String name, Pageable pageable) {
    return contactListRepository.findByUser_IdAndNameContainingIgnoreCase(userId, name, pageable);
  }

  /**
   * Update an existing contact list.
   *
   * @param id the list ID
   * @param userId the user ID
   * @param updatedList the updated list data
   * @return the updated contact list
   * @throws ResourceNotFoundException if list not found
   */
  public ContactList updateContactList(String id, String userId, ContactList updatedList) {
    ContactList contactList = findByIdAndUserId(id, userId);

    // Check name uniqueness if name is being changed
    if (updatedList.getName() != null && !updatedList.getName().equals(contactList.getName())) {
      if (contactListRepository.findByNameAndUser_Id(updatedList.getName(), userId).isPresent()) {
        throw new ValidationException("Contact list with this name already exists", "name");
      }
      contactList.setName(updatedList.getName());
    }

    if (updatedList.getDescription() != null) {
      contactList.setDescription(updatedList.getDescription());
    }
    if (updatedList.getDoubleOptInEnabled() != null) {
      contactList.setDoubleOptInEnabled(updatedList.getDoubleOptInEnabled());
    }

    contactList.setUpdatedAt(LocalDateTime.now());
    return contactListRepository.save(contactList);
  }

  /**
   * Update contact list statistics.
   *
   * @param id the list ID
   * @param userId the user ID
   * @param totalContacts total number of contacts
   * @param activeContacts number of active contacts
   * @return the updated contact list
   */
  public ContactList updateStatistics(String id, String userId, Integer totalContacts, Integer activeContacts) {
    ContactList contactList = findByIdAndUserId(id, userId);

    if (totalContacts != null) {
      contactList.setTotalContacts(totalContacts);
    }
    if (activeContacts != null) {
      contactList.setActiveContacts(activeContacts);
    }

    contactList.setUpdatedAt(LocalDateTime.now());
    return contactListRepository.save(contactList);
  }

  /**
   * Delete a contact list.
   *
   * @param id the list ID
   * @param userId the user ID
   * @throws ResourceNotFoundException if list not found
   */
  public void deleteContactList(String id, String userId) {
    ContactList contactList = findByIdAndUserId(id, userId);
    contactListRepository.delete(contactList);
  }

  /**
   * Delete all contact lists for a user (GDPR compliance).
   *
   * @param userId the user ID
   */
  public void deleteAllByUserId(String userId) {
    contactListRepository.deleteByUser_Id(userId);
  }

  /**
   * Count contact lists for a user.
   *
   * @param userId the user ID
   * @return count of contact lists
   */
  @Transactional(readOnly = true)
  public long countByUserId(String userId) {
    return contactListRepository.countByUser_Id(userId);
  }
}
