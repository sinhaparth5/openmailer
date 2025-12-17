package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EmailTemplate entity.
 * Provides CRUD operations and custom query methods for email templates.
 */
@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

  /**
   * Find all templates for a specific user.
   *
   * @param userId the user ID
   * @return list of email templates
   */
  List<EmailTemplate> findByUserId(Long userId);

  /**
   * Find all templates for a specific user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of email templates
   */
  Page<EmailTemplate> findByUserId(Long userId, Pageable pageable);

  /**
   * Find template by ID and user ID.
   *
   * @param id the template ID
   * @param userId the user ID
   * @return Optional containing the template if found
   */
  Optional<EmailTemplate> findByIdAndUserId(Long id, Long userId);

  /**
   * Find templates by name containing search term.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching templates
   */
  Page<EmailTemplate> findByUserIdAndNameContainingIgnoreCase(Long userId, String name, Pageable pageable);

  /**
   * Count templates for a specific user.
   *
   * @param userId the user ID
   * @return count of templates
   */
  long countByUserId(Long userId);

  /**
   * Delete all templates for a specific user.
   *
   * @param userId the user ID
   */
  void deleteByUserId(Long userId);
}
