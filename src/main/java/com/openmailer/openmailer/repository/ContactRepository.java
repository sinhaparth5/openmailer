package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Contact entity.
 * Provides CRUD operations and custom query methods for contact management.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

  /**
   * Find all contacts for a specific user.
   *
   * NOTE: Uses JPA relationship navigation (user.id),
   * not a physical userId column on the entity.
   *
   * @param userId the user ID
   * @return list of contacts
   */
  List<Contact> findByUser_Id(Long userId);

  /**
   * Find all contacts for a specific user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of contacts
   */
  Page<Contact> findByUser_Id(Long userId, Pageable pageable);

  /**
   * Find contact by ID and user ID.
   *
   * @param id the contact ID
   * @param userId the user ID
   * @return Optional containing the contact if found
   */
  Optional<Contact> findByIdAndUser_Id(Long id, Long userId);

  /**
   * Find contact by email and user ID.
   *
   * @param email the email address
   * @param userId the user ID
   * @return Optional containing the contact if found
   */
  Optional<Contact> findByEmailAndUser_Id(String email, Long userId);

  /**
   * Find contacts by status.
   *
   * @param userId the user ID
   * @param status the contact status (SUBSCRIBED, UNSUBSCRIBED, BOUNCED, etc.)
   * @param pageable pagination information
   * @return page of contacts
   */
  Page<Contact> findByUser_IdAndStatus(Long userId, String status, Pageable pageable);

  /**
   * Search contacts by email, first name, or last name.
   *
   * Uses JPQL and entity relationship navigation (c.user.id).
   *
   * @param userId the user ID
   * @param searchTerm the search term
   * @param pageable pagination information
   * @return page of matching contacts
   */
  @Query("""
      SELECT c FROM Contact c
      WHERE c.user.id = :userId AND (
          LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
          LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
          LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
      )
  """)
  Page<Contact> searchContacts(@Param("userId") Long userId,
                               @Param("searchTerm") String searchTerm,
                               Pageable pageable);

  /**
   * Find contacts by tag.
   *
   * NOTE:
   * - Uses a native PostgreSQL query
   * - Required because tags are stored as TEXT[]
   * - JPQL does NOT support SQL array columns
   *
   * @param userId the user ID
   * @param tag the tag to search for
   * @param pageable pagination information
   * @return page of contacts with the tag
   */
  @Query(
      value = """
          SELECT * FROM contacts c
          WHERE c.user_id = :userId
          AND :tag = ANY(c.tags)
      """,
      countQuery = """
          SELECT COUNT(*) FROM contacts c
          WHERE c.user_id = :userId
          AND :tag = ANY(c.tags)
      """,
      nativeQuery = true
  )
  Page<Contact> findByTag(@Param("userId") Long userId,
                          @Param("tag") String tag,
                          Pageable pageable);

  /**
   * Find contacts created after a certain date.
   *
   * @param userId the user ID
   * @param date the date threshold
   * @return list of contacts created after the date
   */
  List<Contact> findByUser_IdAndCreatedAtAfter(Long userId, LocalDateTime date);

  /**
   * Find contacts by GDPR consent status.
   *
   * @param userId the user ID
   * @param gdprConsent consent status
   * @param pageable pagination information
   * @return page of contacts
   */
  Page<Contact> findByUser_IdAndGdprConsent(Long userId, Boolean gdprConsent, Pageable pageable);

  /**
   * Count contacts for a specific user.
   *
   * @param userId the user ID
   * @return count of contacts
   */
  long countByUser_Id(Long userId);

  /**
   * Count contacts by status.
   *
   * @param userId the user ID
   * @param status the contact status
   * @return count of contacts with given status
   */
  long countByUser_IdAndStatus(Long userId, String status);

  /**
   * Check if email exists for a user.
   *
   * @param email the email address
   * @param userId the user ID
   * @return true if email exists, false otherwise
   */
  boolean existsByEmailAndUser_Id(String email, Long userId);

  /**
   * Delete all contacts for a specific user.
   *
   * @param userId the user ID
   */
  void deleteByUser_Id(Long userId);
}
