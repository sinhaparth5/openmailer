package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.Segment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Segment entity.
 * Provides CRUD operations and custom query methods for managing contact segments.
 */
@Repository
public interface SegmentRepository extends JpaRepository<Segment, Long> {

  /**
   * Find all segments for a specific user.
   *
   * @param userId the user ID
   * @return list of segments
   */
  @Query("SELECT s FROM Segment s WHERE s.user.id = :userId")
  List<Segment> findByUserId(@Param("userId") Long userId);

  /**
   * Find all segments for a specific user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of segments
   */
  @Query("SELECT s FROM Segment s WHERE s.user.id = :userId")
  Page<Segment> findByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * Find segment by ID and user ID.
   *
   * @param id the segment ID
   * @param userId the user ID
   * @return Optional containing the segment if found
   */
  @Query("SELECT s FROM Segment s WHERE s.id = :id AND s.user.id = :userId")
  Optional<Segment> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  /**
   * Find segment by name and user ID.
   *
   * @param name the segment name
   * @param userId the user ID
   * @return Optional containing the segment if found
   */
  @Query("SELECT s FROM Segment s WHERE s.name = :name AND s.user.id = :userId")
  Optional<Segment> findByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);

  /**
   * Find all dynamic segments for a user.
   *
   * @param userId the user ID
   * @param isDynamic true for dynamic segments, false for static
   * @param pageable pagination information
   * @return page of segments
   */
  @Query("SELECT s FROM Segment s WHERE s.user.id = :userId AND s.isDynamic = :isDynamic")
  Page<Segment> findByUserIdAndIsDynamic(@Param("userId") Long userId, @Param("isDynamic") Boolean isDynamic, Pageable pageable);

  /**
   * Find all segments for a specific contact list.
   *
   * @param listId the contact list ID
   * @return list of segments
   */
  @Query("SELECT s FROM Segment s WHERE s.contactList.id = :listId")
  List<Segment> findByContactListId(@Param("listId") Long listId);

  /**
   * Search segments by name.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching segments
   */
  @Query("SELECT s FROM Segment s WHERE s.user.id = :userId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
  Page<Segment> findByUserIdAndNameContainingIgnoreCase(@Param("userId") Long userId, @Param("name") String name, Pageable pageable);

  /**
   * Count segments for a specific user.
   *
   * @param userId the user ID
   * @return count of segments
   */
  @Query("SELECT COUNT(s) FROM Segment s WHERE s.user.id = :userId")
  long countByUserId(@Param("userId") Long userId);

  /**
   * Delete all segments for a specific user.
   *
   * @param userId the user ID
   */
  @Query("DELETE FROM Segment s WHERE s.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  /**
   * Delete all segments for a specific contact list.
   *
   * @param listId the contact list ID
   */
  @Query("DELETE FROM Segment s WHERE s.contactList.id = :listId")
  void deleteByContactListId(@Param("listId") Long listId);
}
