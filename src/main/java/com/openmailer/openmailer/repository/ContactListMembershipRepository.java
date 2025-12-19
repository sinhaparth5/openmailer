package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.ContactListMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ContactListMembership entity.
 * Provides CRUD operations and custom query methods for managing contact-list relationships.
 */
@Repository
public interface ContactListMembershipRepository extends JpaRepository<ContactListMembership, String> {

  /**
   * Find membership by contact ID and list ID.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   * @return Optional containing the membership if found
   */
  Optional<ContactListMembership> findByContactIdAndListId(String contactId, String listId);

  /**
   * Find all memberships for a contact.
   *
   * @param contactId the contact ID
   * @return list of memberships
   */
  List<ContactListMembership> findByContactId(String contactId);

  /**
   * Find all memberships for a list.
   *
   * @param listId the list ID
   * @return list of memberships
   */
  List<ContactListMembership> findByListId(String listId);

  /**
   * Find all memberships for a list with pagination.
   *
   * @param listId the list ID
   * @param pageable pagination information
   * @return page of memberships
   */
  Page<ContactListMembership> findByListId(String listId, Pageable pageable);

  /**
   * Find memberships by status.
   *
   * @param listId the list ID
   * @param status the membership status
   * @param pageable pagination information
   * @return page of memberships
   */
  Page<ContactListMembership> findByListIdAndStatus(String listId, String status, Pageable pageable);

  /**
   * Get all contact IDs for a list.
   *
   * @param listId the list ID
   * @return list of contact IDs
   */
  @Query("SELECT m.contactId FROM ContactListMembership m WHERE m.listId = :listId")
  List<String> findContactIdsByListId(@Param("listId") String listId);

  /**
   * Get all active contact IDs for a list.
   *
   * @param listId the list ID
   * @param status the membership status
   * @return list of contact IDs
   */
  @Query("SELECT m.contactId FROM ContactListMembership m WHERE m.listId = :listId AND m.status = :status")
  List<String> findContactIdsByListIdAndStatus(@Param("listId") String listId, @Param("status") String status);

  /**
   * Count memberships for a list.
   *
   * @param listId the list ID
   * @return count of memberships
   */
  long countByListId(String listId);

  /**
   * Count active memberships for a list.
   *
   * @param listId the list ID
   * @param status the membership status
   * @return count of memberships with given status
   */
  long countByListIdAndStatus(String listId, String status);

  /**
   * Check if membership exists.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   * @return true if membership exists, false otherwise
   */
  boolean existsByContactIdAndListId(String contactId, String listId);

  /**
   * Delete membership.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   */
  void deleteByContactIdAndListId(String contactId, String listId);

  /**
   * Delete all memberships for a contact.
   *
   * @param contactId the contact ID
   */
  void deleteByContactId(String contactId);

  /**
   * Delete all memberships for a list.
   *
   * @param listId the list ID
   */
  void deleteByListId(String listId);
}
