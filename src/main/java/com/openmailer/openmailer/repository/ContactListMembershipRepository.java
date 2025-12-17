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
public interface ContactListMembershipRepository extends JpaRepository<ContactListMembership, Long> {

  /**
   * Find membership by contact ID and list ID.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   * @return Optional containing the membership if found
   */
  Optional<ContactListMembership> findByContactIdAndListId(Long contactId, Long listId);

  /**
   * Find all memberships for a contact.
   *
   * @param contactId the contact ID
   * @return list of memberships
   */
  List<ContactListMembership> findByContactId(Long contactId);

  /**
   * Find all memberships for a list.
   *
   * @param listId the list ID
   * @return list of memberships
   */
  List<ContactListMembership> findByListId(Long listId);

  /**
   * Find all memberships for a list with pagination.
   *
   * @param listId the list ID
   * @param pageable pagination information
   * @return page of memberships
   */
  Page<ContactListMembership> findByListId(Long listId, Pageable pageable);

  /**
   * Find memberships by status.
   *
   * @param listId the list ID
   * @param status the membership status
   * @param pageable pagination information
   * @return page of memberships
   */
  Page<ContactListMembership> findByListIdAndStatus(Long listId, String status, Pageable pageable);

  /**
   * Get all contact IDs for a list.
   *
   * @param listId the list ID
   * @return list of contact IDs
   */
  @Query("SELECT m.contactId FROM ContactListMembership m WHERE m.listId = :listId")
  List<Long> findContactIdsByListId(@Param("listId") Long listId);

  /**
   * Get all active contact IDs for a list.
   *
   * @param listId the list ID
   * @param status the membership status
   * @return list of contact IDs
   */
  @Query("SELECT m.contactId FROM ContactListMembership m WHERE m.listId = :listId AND m.status = :status")
  List<Long> findContactIdsByListIdAndStatus(@Param("listId") Long listId, @Param("status") String status);

  /**
   * Count memberships for a list.
   *
   * @param listId the list ID
   * @return count of memberships
   */
  long countByListId(Long listId);

  /**
   * Count active memberships for a list.
   *
   * @param listId the list ID
   * @param status the membership status
   * @return count of memberships with given status
   */
  long countByListIdAndStatus(Long listId, String status);

  /**
   * Check if membership exists.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   * @return true if membership exists, false otherwise
   */
  boolean existsByContactIdAndListId(Long contactId, Long listId);

  /**
   * Delete membership.
   *
   * @param contactId the contact ID
   * @param listId the list ID
   */
  void deleteByContactIdAndListId(Long contactId, Long listId);

  /**
   * Delete all memberships for a contact.
   *
   * @param contactId the contact ID
   */
  void deleteByContactId(Long contactId);

  /**
   * Delete all memberships for a list.
   *
   * @param listId the list ID
   */
  void deleteByListId(Long listId);
}
