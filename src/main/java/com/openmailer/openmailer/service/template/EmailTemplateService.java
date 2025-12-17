package com.openmailer.openmailer.service.template;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.UnauthorizedException;
import com.openmailer.openmailer.model.EmailTemplate;
import com.openmailer.openmailer.repository.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for EmailTemplate management operations.
 * Handles template CRUD operations with user authorization.
 */
@Service
@Transactional
public class EmailTemplateService {

  private final EmailTemplateRepository templateRepository;

  @Autowired
  public EmailTemplateService(EmailTemplateRepository templateRepository) {
    this.templateRepository = templateRepository;
  }

  /**
   * Create a new email template.
   *
   * @param template the template to create
   * @return the created template
   */
  public EmailTemplate createTemplate(EmailTemplate template) {
    template.setCreatedAt(LocalDateTime.now());
    template.setUpdatedAt(LocalDateTime.now());
    return templateRepository.save(template);
  }

  /**
   * Find template by ID.
   *
   * @param id the template ID
   * @return the template
   * @throws ResourceNotFoundException if template not found
   */
  @Transactional(readOnly = true)
  public EmailTemplate findById(Long id) {
    return templateRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", "id", id));
  }

  /**
   * Find template by ID and verify user ownership.
   *
   * @param id the template ID
   * @param userId the user ID
   * @return the template
   * @throws ResourceNotFoundException if template not found
   * @throws UnauthorizedException if user doesn't own the template
   */
  @Transactional(readOnly = true)
  public EmailTemplate findByIdAndUserId(Long id, Long userId) {
    return templateRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", "id", id));
  }

  /**
   * Find all templates for a user.
   *
   * @param userId the user ID
   * @return list of templates
   */
  @Transactional(readOnly = true)
  public List<EmailTemplate> findByUserId(Long userId) {
    return templateRepository.findByUserId(userId);
  }

  /**
   * Find all templates for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of templates
   */
  @Transactional(readOnly = true)
  public Page<EmailTemplate> findByUserId(Long userId, Pageable pageable) {
    return templateRepository.findByUserId(userId, pageable);
  }

  /**
   * Search templates by name.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching templates
   */
  @Transactional(readOnly = true)
  public Page<EmailTemplate> searchByName(Long userId, String name, Pageable pageable) {
    return templateRepository.findByUserIdAndNameContainingIgnoreCase(userId, name, pageable);
  }

  /**
   * Update an existing template.
   *
   * @param id the template ID
   * @param userId the user ID
   * @param updatedTemplate the updated template data
   * @return the updated template
   * @throws ResourceNotFoundException if template not found
   */
  public EmailTemplate updateTemplate(Long id, Long userId, EmailTemplate updatedTemplate) {
    EmailTemplate template = findByIdAndUserId(id, userId);

    if (updatedTemplate.getName() != null) {
      template.setName(updatedTemplate.getName());
    }
    if (updatedTemplate.getSubject() != null) {
      template.setSubject(updatedTemplate.getSubject());
    }
    if (updatedTemplate.getHtmlContent() != null) {
      template.setHtmlContent(updatedTemplate.getHtmlContent());
    }
    if (updatedTemplate.getPlainTextContent() != null) {
      template.setPlainTextContent(updatedTemplate.getPlainTextContent());
    }

    template.setUpdatedAt(LocalDateTime.now());
    return templateRepository.save(template);
  }

  /**
   * Delete a template.
   *
   * @param id the template ID
   * @param userId the user ID
   * @throws ResourceNotFoundException if template not found
   */
  public void deleteTemplate(Long id, Long userId) {
    EmailTemplate template = findByIdAndUserId(id, userId);
    templateRepository.delete(template);
  }

  /**
   * Delete all templates for a user (GDPR compliance).
   *
   * @param userId the user ID
   */
  public void deleteAllByUserId(Long userId) {
    templateRepository.deleteByUserId(userId);
  }

  /**
   * Count templates for a user.
   *
   * @param userId the user ID
   * @return count of templates
   */
  @Transactional(readOnly = true)
  public long countByUserId(Long userId) {
    return templateRepository.countByUserId(userId);
  }
}
