package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for Contact management operations.
 * Handles contact CRUD operations, search, and user authorization.
 */
@Service
@Transactional
public class ContactService {

  private final ContactRepository contactRepository;

  @Autowired
  public ContactService(ContactRepository contactRepository) {
    this.contactRepository = contactRepository;
  }

  /**
   * Create a new contact.
   *
   * @param contact the contact to create
   * @return the created contact
   * @throws ValidationException if email already exists for user
   */
  public Contact createContact(Contact contact) {
    String normalizedEmail = normalizeEmail(contact.getEmail());
    contact.setEmail(normalizedEmail);

    if (contactRepository.existsByEmailIgnoreCaseAndUserId(
        normalizedEmail,
        contact.getUser().getId()
    )) {
      throw new ValidationException("A contact with this email already exists for your account.", "email");
    }

    if (contact.getStatus() == null) {
      contact.setStatus("PENDING");
    }

    contact.setCreatedAt(LocalDateTime.now());
    contact.setUpdatedAt(LocalDateTime.now());

    return contactRepository.save(contact);
  }

  /**
   * Find contact by ID.
   */
  @Transactional(readOnly = true)
  public Contact findById(String id) {
    return contactRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", id));
  }

  /**
   * Find contact by ID and verify user ownership.
   */
  @Transactional(readOnly = true)
  public Contact findByIdAndUserId(String id, String userId) {
    return contactRepository.findByIdAndUser_Id(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", id));
  }

  /**
   * Find contact by email.
   */
  @Transactional(readOnly = true)
  public Contact findByEmail(String email, String userId) {
    return contactRepository.findByEmailIgnoreCaseAndUserId(normalizeEmail(email), userId)
        .orElseThrow(() -> new ResourceNotFoundException("Contact", "email", email));
  }

  /**
   * Find all contacts for a user.
   */
  @Transactional(readOnly = true)
  public List<Contact> findByUserId(String userId) {
    return contactRepository.findByUser_Id(userId);
  }

  /**
   * Find all contacts for a user with pagination.
   */
  @Transactional(readOnly = true)
  public Page<Contact> findByUserId(String userId, Pageable pageable) {
    return contactRepository.findByUser_Id(userId, pageable);
  }

  /**
   * Find contacts by status.
   */
  @Transactional(readOnly = true)
  public Page<Contact> findByStatus(String userId, String status, Pageable pageable) {
    return contactRepository.findByUser_IdAndStatus(userId, status, pageable);
  }

  /**
   * Search contacts by email, first name, or last name.
   */
  @Transactional(readOnly = true)
  public Page<Contact> searchContacts(String userId, String searchTerm, Pageable pageable) {
    return contactRepository.searchContacts(userId, searchTerm, pageable);
  }

  /**
   * Find contacts by tag.
   */
  @Transactional(readOnly = true)
  public Page<Contact> findByTag(String userId, String tag, Pageable pageable) {
    return contactRepository.findByTag(userId, tag, pageable);
  }

  /**
   * Update an existing contact.
   */
  public Contact updateContact(String id, String userId, Contact updatedContact) {

    Contact contact = findByIdAndUserId(id, userId);

    String normalizedExistingEmail = normalizeEmail(contact.getEmail());
    String normalizedUpdatedEmail = normalizeEmail(updatedContact.getEmail());

    if (normalizedUpdatedEmail != null &&
        !normalizedUpdatedEmail.equals(normalizedExistingEmail)) {

      if (contactRepository.existsByEmailIgnoreCaseAndUserId(normalizedUpdatedEmail, userId)) {
        throw new ValidationException("A contact with this email already exists for your account.", "email");
      }

      contact.setEmail(normalizedUpdatedEmail);
    }

    contact.setFirstName(updatedContact.getFirstName());
    contact.setLastName(updatedContact.getLastName());
    contact.setStatus(updatedContact.getStatus());
    contact.setTags(updatedContact.getTags());
    contact.setCustomFields(updatedContact.getCustomFields());
    contact.setNotes(updatedContact.getNotes());
    contact.setGdprConsent(updatedContact.getGdprConsent());

    contact.setUpdatedAt(LocalDateTime.now());
    return contactRepository.save(contact);
  }

  /**
   * Subscribe a contact.
   */
  public Contact subscribe(String id, String userId) {
    Contact contact = findByIdAndUserId(id, userId);
    contact.setStatus("SUBSCRIBED");
    contact.setSubscribedAt(LocalDateTime.now());
    contact.setUpdatedAt(LocalDateTime.now());
    return contactRepository.save(contact);
  }

  /**
   * Unsubscribe a contact.
   */
  public Contact unsubscribe(String id, String userId, String reason) {
    Contact contact = findByIdAndUserId(id, userId);
    contact.setStatus("UNSUBSCRIBED");
    contact.setUnsubscribedAt(LocalDateTime.now());
    contact.setUnsubscribeReason(reason);
    contact.setUpdatedAt(LocalDateTime.now());
    return contactRepository.save(contact);
  }

  /**
   * Mark contact as bounced.
   */
  public Contact markAsBounced(String id, String userId) {
    Contact contact = findByIdAndUserId(id, userId);
    contact.setStatus("BOUNCED");
    contact.setBounceCount(contact.getBounceCount() + 1);
    contact.setLastBouncedAt(LocalDateTime.now());
    contact.setUpdatedAt(LocalDateTime.now());
    return contactRepository.save(contact);
  }

  /**
   * Delete a contact.
   */
  public void deleteContact(String id, String userId) {
    Contact contact = findByIdAndUserId(id, userId);
    contactRepository.delete(contact);
  }

  /**
   * Delete all contacts for a user (GDPR compliance).
   */
  public void deleteAllByUserId(String userId) {
    contactRepository.deleteByUser_Id(userId);
  }

  /**
   * Count contacts for a user.
   */
  @Transactional(readOnly = true)
  public long countByUserId(String userId) {
    return contactRepository.countByUser_Id(userId);
  }

  /**
   * Count contacts by status.
   */
  @Transactional(readOnly = true)
  public long countByStatus(String userId, String status) {
    return contactRepository.countByUser_IdAndStatus(userId, status);
  }

  /**
   * Check if email exists for a user.
   */
  @Transactional(readOnly = true)
  public boolean emailExists(String email, String userId) {
    return contactRepository.existsByEmailIgnoreCaseAndUserId(normalizeEmail(email), userId);
  }

  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }

  /**
   * Count contacts in a list by status.
   */
  @Transactional(readOnly = true)
  public long countByListAndStatus(String listId, String status) {
    return contactRepository.countByListAndStatus(listId, status);
  }
}
