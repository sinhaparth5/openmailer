package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.ContactList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ContactList entity.
 * Provides CRUD operations and custom query methods for contact list management.
 */
@Repository
public interface ContactListRepository extends JpaRepository<ContactList, String> {

  /**
   * Find all contact lists for a specific user.
   *
   * @param userId the user ID
   * @return list of contact lists
   */
  List<ContactList> findByUser_Id(String userId);

  /**
   * Find all contact lists for a specific user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of contact lists
   */
  Page<ContactList> findByUser_Id(String userId, Pageable pageable);

  /**
   * Find contact list by ID and user ID.
   *
   * @param id the list ID
   * @param userId the user ID
   * @return Optional containing the list if found
   */
  Optional<ContactList> findByIdAndUser_Id(String id, String userId);

  /**
   * Find contact list by name and user ID.
   *
   * @param name the list name
   * @param userId the user ID
   * @return Optional containing the list if found
   */
  Optional<ContactList> findByNameAndUser_Id(String name, String userId);

  /**
   * Search contact lists by name.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching contact lists
   */
  Page<ContactList> findByUser_IdAndNameContainingIgnoreCase(String userId, String name, Pageable pageable);

  /**
   * Count contact lists for a specific user.
   *
   * @param userId the user ID
   * @return count of contact lists
   */
  long countByUser_Id(String userId);

  /**
   * Delete all contact lists for a specific user.
   *
   * @param userId the user ID
   */
  void deleteByUser_Id(String userId);
}
