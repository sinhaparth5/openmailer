package com.openmailer.openmailer.service.email;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.model.EmailLog;
import com.openmailer.openmailer.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for EmailLog management operations.
 * Handles email logging, auditing, and reporting.
 */
@Service
@Transactional
public class EmailLogService {

  private final EmailLogRepository logRepository;

  @Autowired
  public EmailLogService(EmailLogRepository logRepository) {
    this.logRepository = logRepository;
  }

  /**
   * Create a new email log entry.
   *
   * @param log the log to create
   * @return the created log
   */
  public EmailLog createLog(EmailLog log) {
    return logRepository.save(log);
  }

  /**
   * Find log by ID.
   *
   * @param id the ID (String)
   * @return the log
   * @throws ResourceNotFoundException if log not found
   */
  @Transactional(readOnly = true)
  public EmailLog findById(String id) {
    return logRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmailLog", "id", id));
  }

  /**
   * Find log by provider message ID.
   *
   * @param providerMessageId the ID (String)
   * @return the log
   * @throws ResourceNotFoundException if log not found
   */
  @Transactional(readOnly = true)
  public EmailLog findByProviderMessageId(String providerMessageId) {
    return logRepository.findByProviderMessageId(providerMessageId)
        .orElseThrow(() -> new ResourceNotFoundException("EmailLog", "providerMessageId", providerMessageId));
  }

  /**
   * Find all logs for a user.
   *
   * @param userId the ID (String)
   * @return list of logs
   */
  @Transactional(readOnly = true)
  public List<EmailLog> findByUser(String userId) {
    return logRepository.findByUserId(userId);
  }

  /**
   * Find all logs for a user with pagination.
   *
   * @param userId the ID (String)
   * @param pageable pagination information
   * @return page of logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findByUser(String userId, Pageable pageable) {
    return logRepository.findByUserId(userId, pageable);
  }

  /**
   * Find all logs for a campaign.
   *
   * @param campaignId the ID (String)
   * @return list of logs
   */
  @Transactional(readOnly = true)
  public List<EmailLog> findByCampaign(String campaignId) {
    return logRepository.findByCampaignId(campaignId);
  }

  /**
   * Find all logs for a campaign with pagination.
   *
   * @param campaignId the ID (String)
   * @param pageable pagination information
   * @return page of logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findByCampaign(String campaignId, Pageable pageable) {
    return logRepository.findByCampaignId(campaignId, pageable);
  }

  /**
   * Find all logs for a recipient.
   *
   * @param recipientId the ID (String)
   * @return list of logs
   */
  @Transactional(readOnly = true)
  public List<EmailLog> findByRecipient(String recipientId) {
    return logRepository.findByRecipientId(recipientId);
  }

  /**
   * Find logs by status.
   *
   * @param userId the ID (String)
   * @param status the email status
   * @param pageable pagination information
   * @return page of logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findByStatus(String userId, String status, Pageable pageable) {
    return logRepository.findByUserIdAndStatus(userId, status, pageable);
  }

  /**
   * Find logs by campaign and status.
   *
   * @param campaignId the ID (String)
   * @param status the email status
   * @param pageable pagination information
   * @return page of logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findByCampaignAndStatus(String campaignId, String status, Pageable pageable) {
    return logRepository.findByCampaignIdAndStatus(campaignId, status, pageable);
  }

  /**
   * Find logs by email type.
   *
   * @param userId the ID (String)
   * @param emailType the email type
   * @param pageable pagination information
   * @return page of logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findByEmailType(String userId, String emailType, Pageable pageable) {
    return logRepository.findByEmailType(userId, emailType, pageable);
  }

  /**
   * Find logs within a date range.
   *
   * @param userId the ID (String)
   * @param startDate start date
   * @param endDate end date
   * @param pageable pagination information
   * @return page of logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
    return logRepository.findByDateRange(userId, startDate, endDate, pageable);
  }

  /**
   * Find logs by recipient email.
   *
   * @param userId the ID (String)
   * @param recipientEmail the recipient email
   * @param pageable pagination information
   * @return page of logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findByRecipientEmail(String userId, String recipientEmail, Pageable pageable) {
    return logRepository.findByRecipientEmail(userId, recipientEmail, pageable);
  }

  /**
   * Find failed logs for retry.
   *
   * @param userId the ID (String)
   * @param pageable pagination information
   * @return page of failed logs
   */
  @Transactional(readOnly = true)
  public Page<EmailLog> findFailedLogs(String userId, Pageable pageable) {
    return logRepository.findFailedLogs(userId, pageable);
  }

  /**
   * Update log status.
   *
   * @param id the ID (String)
   * @param status the new status
   * @return the updated log
   */
  public EmailLog updateStatus(String id, String status) {
    EmailLog log = findById(id);
    log.setStatus(status);
    return logRepository.save(log);
  }

  /**
   * Mark log as sent.
   *
   * @param id the ID (String)
   * @return the updated log
   */
  public EmailLog markAsSent(String id) {
    EmailLog log = findById(id);
    log.setStatus("SENT");
    log.setSentAt(LocalDateTime.now());
    return logRepository.save(log);
  }

  /**
   * Mark log as delivered.
   *
   * @param id the ID (String)
   * @return the updated log
   */
  public EmailLog markAsDelivered(String id) {
    EmailLog log = findById(id);
    log.setStatus("DELIVERED");
    log.setDeliveredAt(LocalDateTime.now());
    return logRepository.save(log);
  }

  /**
   * Mark log as failed.
   *
   * @param id the ID (String)
   * @param errorMessage the error message
   * @return the updated log
   */
  public EmailLog markAsFailed(String id, String errorMessage) {
    EmailLog log = findById(id);
    log.setStatus("FAILED");
    log.setErrorMessage(errorMessage);
    return logRepository.save(log);
  }

  /**
   * Count logs for a user.
   *
   * @param userId the ID (String)
   * @return count of logs
   */
  @Transactional(readOnly = true)
  public long countByUser(String userId) {
    return logRepository.countByUserId(userId);
  }

  /**
   * Count logs for a campaign.
   *
   * @param campaignId the ID (String)
   * @return count of logs
   */
  @Transactional(readOnly = true)
  public long countByCampaign(String campaignId) {
    return logRepository.countByCampaignId(campaignId);
  }

  /**
   * Count logs by status.
   *
   * @param userId the ID (String)
   * @param status the email status
   * @return count of logs
   */
  @Transactional(readOnly = true)
  public long countByStatus(String userId, String status) {
    return logRepository.countByUserIdAndStatus(userId, status);
  }

  /**
   * Count logs by email type.
   *
   * @param userId the ID (String)
   * @param emailType the email type
   * @return count of logs
   */
  @Transactional(readOnly = true)
  public long countByEmailType(String userId, String emailType) {
    return logRepository.countByEmailType(userId, emailType);
  }

  /**
   * Delete all logs for a user (GDPR compliance).
   *
   * @param userId the ID (String)
   */
  public void deleteAllByUser(String userId) {
    logRepository.deleteByUserId(userId);
  }

  /**
   * Delete all logs for a campaign.
   *
   * @param campaignId the ID (String)
   */
  public void deleteAllByCampaign(String campaignId) {
    logRepository.deleteByCampaignId(campaignId);
  }
}
