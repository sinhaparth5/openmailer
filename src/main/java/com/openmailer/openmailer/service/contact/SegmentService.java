package com.openmailer.openmailer.service.contact;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Segment;
import com.openmailer.openmailer.repository.SegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service class for Segment management operations.
 * Handles segment CRUD operations, condition evaluation, and contact counting.
 */
@Service
@Transactional
public class SegmentService {

  private final SegmentRepository segmentRepository;

  @Autowired
  public SegmentService(SegmentRepository segmentRepository) {
    this.segmentRepository = segmentRepository;
  }

  /**
   * Create a new segment.
   *
   * @param segment the segment to create
   * @return the created segment
   * @throws ValidationException if segment name already exists for user
   */
  @Caching(evict = {
      @CacheEvict(value = "segmentCounts", allEntries = true),
      @CacheEvict(value = "segments", allEntries = true)
  })
  public Segment createSegment(Segment segment) {
    // Validate segment name uniqueness for user
    String userId = segment.getUser().getId();
    if (segmentRepository.findByNameAndUserId(segment.getName(), userId).isPresent()) {
      throw new ValidationException("Segment with this name already exists", "name");
    }

    // Set default values
    if (segment.getIsDynamic() == null) {
      segment.setIsDynamic(true);
    }
    if (segment.getCachedCount() == null) {
      segment.setCachedCount(0);
    }

    segment.setCreatedAt(LocalDateTime.now());
    segment.setUpdatedAt(LocalDateTime.now());

    return segmentRepository.save(segment);
  }

  /**
   * Find segment by ID.
   *
   * @param id the segment ID
   * @return the segment
   * @throws ResourceNotFoundException if segment not found
   */
  @Cacheable(value = "segmentCounts", key = "'segment:' + #id")
  @Transactional(readOnly = true)
  public Segment findById(String id) {
    return segmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Segment", "id", id));
  }

  /**
   * Find segment by ID and verify user ownership.
   *
   * @param id the segment ID
   * @param userId the user ID
   * @return the segment
   * @throws ResourceNotFoundException if segment not found
   */
  @Cacheable(value = "segmentCounts", key = "'segment:' + #id + ':user:' + #userId")
  @Transactional(readOnly = true)
  public Segment findByIdAndUserId(String id, String userId) {
    return segmentRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Segment", "id", id));
  }

  /**
   * Find all segments for a user.
   *
   * @param userId the user ID
   * @return list of segments
   */
  @Transactional(readOnly = true)
  public List<Segment> findByUserId(String userId) {
    return segmentRepository.findByUserId(userId);
  }

  /**
   * Find all segments for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of segments
   */
  @Transactional(readOnly = true)
  public Page<Segment> findByUserId(String userId, Pageable pageable) {
    return segmentRepository.findByUserId(userId, pageable);
  }

  /**
   * Find segments by type (dynamic or static).
   *
   * @param userId the user ID
   * @param isDynamic true for dynamic, false for static
   * @param pageable pagination information
   * @return page of segments
   */
  @Transactional(readOnly = true)
  public Page<Segment> findByType(String userId, Boolean isDynamic, Pageable pageable) {
    return segmentRepository.findByUserIdAndIsDynamic(userId, isDynamic, pageable);
  }

  /**
   * Search segments by name.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching segments
   */
  @Transactional(readOnly = true)
  public Page<Segment> searchByName(String userId, String name, Pageable pageable) {
    return segmentRepository.findByUserIdAndNameContainingIgnoreCase(userId, name, pageable);
  }

  /**
   * Find all segments for a contact list.
   *
   * @param listId the contact list ID
   * @return list of segments
   */
  @Transactional(readOnly = true)
  public List<Segment> findByContactListId(String listId) {
    return segmentRepository.findByContactListId(listId);
  }

  /**
   * Update an existing segment.
   *
   * @param id the segment ID
   * @param userId the user ID
   * @param updatedSegment the updated segment data
   * @return the updated segment
   * @throws ResourceNotFoundException if segment not found
   */
  @Caching(evict = {
      @CacheEvict(value = "segmentCounts", allEntries = true),
      @CacheEvict(value = "segments", allEntries = true)
  })
  public Segment updateSegment(String id, String userId, Segment updatedSegment) {
    Segment segment = findByIdAndUserId(id, userId);

    // Check name uniqueness if name is being changed
    if (updatedSegment.getName() != null && !updatedSegment.getName().equals(segment.getName())) {
      if (segmentRepository.findByNameAndUserId(updatedSegment.getName(), userId).isPresent()) {
        throw new ValidationException("Segment with this name already exists", "name");
      }
      segment.setName(updatedSegment.getName());
    }

    if (updatedSegment.getDescription() != null) {
      segment.setDescription(updatedSegment.getDescription());
    }
    if (updatedSegment.getConditions() != null) {
      segment.setConditions(updatedSegment.getConditions());
      // Reset cached count when conditions change
      segment.setCachedCount(0);
      segment.setLastCalculatedAt(null);
    }
    if (updatedSegment.getIsDynamic() != null) {
      segment.setIsDynamic(updatedSegment.getIsDynamic());
    }
    if (updatedSegment.getContactList() != null) {
      segment.setContactList(updatedSegment.getContactList());
    }

    segment.setUpdatedAt(LocalDateTime.now());
    return segmentRepository.save(segment);
  }

  /**
   * Update segment conditions.
   *
   * @param id the segment ID
   * @param userId the user ID
   * @param conditions the new conditions
   * @return the updated segment
   */
  @Caching(evict = {
      @CacheEvict(value = "segmentCounts", allEntries = true),
      @CacheEvict(value = "segments", allEntries = true)
  })
  public Segment updateConditions(String id, String userId, Map<String, Object> conditions) {
    Segment segment = findByIdAndUserId(id, userId);
    segment.setConditions(conditions);
    segment.setCachedCount(0);
    segment.setLastCalculatedAt(null);
    segment.setUpdatedAt(LocalDateTime.now());
    return segmentRepository.save(segment);
  }

  /**
   * Update segment cached count.
   *
   * @param id the segment ID
   * @param userId the user ID
   * @param count the contact count
   * @return the updated segment
   */
  @Caching(evict = {
      @CacheEvict(value = "segmentCounts", allEntries = true),
      @CacheEvict(value = "segments", allEntries = true)
  })
  public Segment updateCachedCount(String id, String userId, Integer count) {
    Segment segment = findByIdAndUserId(id, userId);
    segment.setCachedCount(count);
    segment.setLastCalculatedAt(LocalDateTime.now());
    segment.setUpdatedAt(LocalDateTime.now());
    return segmentRepository.save(segment);
  }

  /**
   * Delete a segment.
   *
   * @param id the segment ID
   * @param userId the user ID
   * @throws ResourceNotFoundException if segment not found
   */
  @Caching(evict = {
      @CacheEvict(value = "segmentCounts", allEntries = true),
      @CacheEvict(value = "segments", allEntries = true)
  })
  public void deleteSegment(String id, String userId) {
    Segment segment = findByIdAndUserId(id, userId);
    segmentRepository.delete(segment);
  }

  /**
   * Delete all segments for a user (GDPR compliance).
   *
   * @param userId the user ID
   */
  @Caching(evict = {
      @CacheEvict(value = "segmentCounts", allEntries = true),
      @CacheEvict(value = "segments", allEntries = true)
  })
  public void deleteAllByUserId(String userId) {
    segmentRepository.deleteByUserId(userId);
  }

  /**
   * Delete all segments for a contact list.
   *
   * @param listId the contact list ID
   */
  @Caching(evict = {
      @CacheEvict(value = "segmentCounts", allEntries = true),
      @CacheEvict(value = "segments", allEntries = true)
  })
  public void deleteByContactListId(String listId) {
    segmentRepository.deleteByContactListId(listId);
  }

  /**
   * Count segments for a user.
   *
   * @param userId the user ID
   * @return count of segments
   */
  @Transactional(readOnly = true)
  public long countByUserId(String userId) {
    return segmentRepository.countByUserId(userId);
  }
}
