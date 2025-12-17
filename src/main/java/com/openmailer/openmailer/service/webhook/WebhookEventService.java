package com.openmailer.openmailer.service.webhook;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.WebhookEvent;
import com.openmailer.openmailer.repository.WebhookEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service class for WebhookEvent management operations.
 * Handles webhook event processing, tracking, and cleanup.
 */
@Service
@Transactional
public class WebhookEventService {

  private final WebhookEventRepository webhookEventRepository;

  @Autowired
  public WebhookEventService(WebhookEventRepository webhookEventRepository) {
    this.webhookEventRepository = webhookEventRepository;
  }

  /**
   * Create a new webhook event.
   *
   * @param event the webhook event to create
   * @return the created webhook event
   * @throws ValidationException if provider event ID already exists
   */
  public WebhookEvent createEvent(WebhookEvent event) {
    // Check for duplicate provider event ID
    if (event.getProviderEventId() != null &&
        webhookEventRepository.existsByProviderEventId(event.getProviderEventId())) {
      throw new ValidationException("Webhook event with this provider event ID already exists", "providerEventId");
    }

    // Set defaults
    if (event.getProcessed() == null) {
      event.setProcessed(false);
    }

    return webhookEventRepository.save(event);
  }

  /**
   * Find webhook event by ID.
   *
   * @param id the webhook event ID
   * @return the webhook event
   * @throws ResourceNotFoundException if webhook event not found
   */
  @Transactional(readOnly = true)
  public WebhookEvent findById(Long id) {
    return webhookEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", "id", id));
  }

  /**
   * Find webhook event by provider event ID.
   *
   * @param providerEventId the provider event ID
   * @return the webhook event
   * @throws ResourceNotFoundException if webhook event not found
   */
  @Transactional(readOnly = true)
  public WebhookEvent findByProviderEventId(String providerEventId) {
    return webhookEventRepository.findByProviderEventId(providerEventId)
        .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", "providerEventId", providerEventId));
  }

  /**
   * Find all webhook events for a user.
   *
   * @param userId the user ID
   * @return list of webhook events
   */
  @Transactional(readOnly = true)
  public List<WebhookEvent> findByUser(Long userId) {
    return webhookEventRepository.findByUserId(userId);
  }

  /**
   * Find all webhook events for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Transactional(readOnly = true)
  public Page<WebhookEvent> findByUser(Long userId, Pageable pageable) {
    return webhookEventRepository.findByUserId(userId, pageable);
  }

  /**
   * Find webhook events by event type.
   *
   * @param userId the user ID
   * @param eventType the event type
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Transactional(readOnly = true)
  public Page<WebhookEvent> findByEventType(Long userId, String eventType, Pageable pageable) {
    return webhookEventRepository.findByEventType(userId, eventType, pageable);
  }

  /**
   * Find webhook events by provider.
   *
   * @param providerId the provider ID
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Transactional(readOnly = true)
  public Page<WebhookEvent> findByProvider(Long providerId, Pageable pageable) {
    return webhookEventRepository.findByProviderId(providerId, pageable);
  }

  /**
   * Find unprocessed webhook events for a user.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of unprocessed webhook events
   */
  @Transactional(readOnly = true)
  public Page<WebhookEvent> findUnprocessed(Long userId, Pageable pageable) {
    return webhookEventRepository.findUnprocessed(userId, pageable);
  }

  /**
   * Find all unprocessed webhook events.
   *
   * @return list of unprocessed webhook events
   */
  @Transactional(readOnly = true)
  public List<WebhookEvent> findAllUnprocessed() {
    return webhookEventRepository.findAllUnprocessed();
  }

  /**
   * Find processed webhook events for a user.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of processed webhook events
   */
  @Transactional(readOnly = true)
  public Page<WebhookEvent> findProcessed(Long userId, Pageable pageable) {
    return webhookEventRepository.findProcessed(userId, pageable);
  }

  /**
   * Find failed webhook events for a user.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of failed webhook events
   */
  @Transactional(readOnly = true)
  public Page<WebhookEvent> findFailed(Long userId, Pageable pageable) {
    return webhookEventRepository.findFailed(userId, pageable);
  }

  /**
   * Find webhook events within a date range.
   *
   * @param userId the user ID
   * @param startDate start date
   * @param endDate end date
   * @param pageable pagination information
   * @return page of webhook events
   */
  @Transactional(readOnly = true)
  public Page<WebhookEvent> findByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
    return webhookEventRepository.findByDateRange(userId, startDate, endDate, pageable);
  }

  /**
   * Mark webhook event as processed.
   *
   * @param id the webhook event ID
   * @return the updated webhook event
   */
  public WebhookEvent markAsProcessed(Long id) {
    WebhookEvent event = findById(id);
    event.setProcessed(true);
    event.setProcessedAt(LocalDateTime.now());
    return webhookEventRepository.save(event);
  }

  /**
   * Mark webhook event as failed.
   *
   * @param id the webhook event ID
   * @param errorMessage the error message
   * @return the updated webhook event
   */
  public WebhookEvent markAsFailed(Long id, String errorMessage) {
    WebhookEvent event = findById(id);
    event.setProcessed(true);
    event.setProcessedAt(LocalDateTime.now());
    event.setErrorMessage(errorMessage);
    return webhookEventRepository.save(event);
  }

  /**
   * Update webhook event payload.
   *
   * @param id the webhook event ID
   * @param payload the new payload
   * @return the updated webhook event
   */
  public WebhookEvent updatePayload(Long id, Map<String, Object> payload) {
    WebhookEvent event = findById(id);
    event.setPayload(payload);
    return webhookEventRepository.save(event);
  }

  /**
   * Count webhook events for a user.
   *
   * @param userId the user ID
   * @return count of webhook events
   */
  @Transactional(readOnly = true)
  public long countByUser(Long userId) {
    return webhookEventRepository.countByUserId(userId);
  }

  /**
   * Count unprocessed webhook events for a user.
   *
   * @param userId the user ID
   * @return count of unprocessed webhook events
   */
  @Transactional(readOnly = true)
  public long countUnprocessed(Long userId) {
    return webhookEventRepository.countUnprocessed(userId);
  }

  /**
   * Count webhook events by event type.
   *
   * @param userId the user ID
   * @param eventType the event type
   * @return count of webhook events
   */
  @Transactional(readOnly = true)
  public long countByEventType(Long userId, String eventType) {
    return webhookEventRepository.countByEventType(userId, eventType);
  }

  /**
   * Delete old processed webhook events.
   *
   * @param daysToKeep number of days to keep
   * @return count of deleted events
   */
  public void deleteOldProcessedEvents(int daysToKeep) {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
    webhookEventRepository.deleteOldProcessedEvents(cutoffDate);
  }

  /**
   * Delete all webhook events for a user.
   *
   * @param userId the user ID
   */
  public void deleteAllByUser(Long userId) {
    webhookEventRepository.deleteByUserId(userId);
  }
}
