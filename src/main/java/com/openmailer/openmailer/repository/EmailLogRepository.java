package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.EmailLog;
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
 * Repository interface for EmailLog entity.
 * Provides CRUD operations and custom query methods for email logging and auditing.
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

  /**
   * Find all logs for a user.
   *
   * @param userId the user ID
   * @return list of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.user.id = :userId")
  List<EmailLog> findByUserId(@Param("userId") Long userId);

  /**
   * Find all logs for a user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.user.id = :userId")
  Page<EmailLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * Find all logs for a campaign.
   *
   * @param campaignId the campaign ID
   * @return list of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.campaign.id = :campaignId")
  List<EmailLog> findByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Find all logs for a campaign with pagination.
   *
   * @param campaignId the campaign ID
   * @param pageable pagination information
   * @return page of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.campaign.id = :campaignId")
  Page<EmailLog> findByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

  /**
   * Find all logs for a recipient.
   *
   * @param recipientId the recipient ID
   * @return list of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.recipient.id = :recipientId")
  List<EmailLog> findByRecipientId(@Param("recipientId") Long recipientId);

  /**
   * Find log by provider message ID.
   *
   * @param providerMessageId the provider message ID
   * @return Optional containing the log if found
   */
  Optional<EmailLog> findByProviderMessageId(String providerMessageId);

  /**
   * Find logs by user and status.
   *
   * @param userId the user ID
   * @param status the email status
   * @param pageable pagination information
   * @return page of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.user.id = :userId AND l.status = :status")
  Page<EmailLog> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);

  /**
   * Find logs by campaign and status.
   *
   * @param campaignId the campaign ID
   * @param status the email status
   * @param pageable pagination information
   * @return page of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.campaign.id = :campaignId AND l.status = :status")
  Page<EmailLog> findByCampaignIdAndStatus(@Param("campaignId") Long campaignId, @Param("status") String status, Pageable pageable);

  /**
   * Find logs by email type.
   *
   * @param userId the user ID
   * @param emailType the email type
   * @param pageable pagination information
   * @return page of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.user.id = :userId AND l.emailType = :emailType")
  Page<EmailLog> findByEmailType(@Param("userId") Long userId, @Param("emailType") String emailType, Pageable pageable);

  /**
   * Find logs within a date range.
   *
   * @param userId the user ID
   * @param startDate start date
   * @param endDate end date
   * @param pageable pagination information
   * @return page of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.user.id = :userId AND l.createdAt BETWEEN :startDate AND :endDate")
  Page<EmailLog> findByDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

  /**
   * Find logs by recipient email.
   *
   * @param userId the user ID
   * @param recipientEmail the recipient email
   * @param pageable pagination information
   * @return page of logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.user.id = :userId AND l.recipientEmail = :recipientEmail")
  Page<EmailLog> findByRecipientEmail(@Param("userId") Long userId, @Param("recipientEmail") String recipientEmail, Pageable pageable);

  /**
   * Find failed logs for retry.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of failed logs
   */
  @Query("SELECT l FROM EmailLog l WHERE l.user.id = :userId AND l.status = 'FAILED'")
  Page<EmailLog> findFailedLogs(@Param("userId") Long userId, Pageable pageable);

  /**
   * Count logs for a user.
   *
   * @param userId the user ID
   * @return count of logs
   */
  @Query("SELECT COUNT(l) FROM EmailLog l WHERE l.user.id = :userId")
  long countByUserId(@Param("userId") Long userId);

  /**
   * Count logs for a campaign.
   *
   * @param campaignId the campaign ID
   * @return count of logs
   */
  @Query("SELECT COUNT(l) FROM EmailLog l WHERE l.campaign.id = :campaignId")
  long countByCampaignId(@Param("campaignId") Long campaignId);

  /**
   * Count logs by status.
   *
   * @param userId the user ID
   * @param status the email status
   * @return count of logs
   */
  @Query("SELECT COUNT(l) FROM EmailLog l WHERE l.user.id = :userId AND l.status = :status")
  long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

  /**
   * Count logs by email type.
   *
   * @param userId the user ID
   * @param emailType the email type
   * @return count of logs
   */
  @Query("SELECT COUNT(l) FROM EmailLog l WHERE l.user.id = :userId AND l.emailType = :emailType")
  long countByEmailType(@Param("userId") Long userId, @Param("emailType") String emailType);

  /**
   * Delete all logs for a user (GDPR compliance).
   *
   * @param userId the user ID
   */
  @Query("DELETE FROM EmailLog l WHERE l.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  /**
   * Delete all logs for a campaign.
   *
   * @param campaignId the campaign ID
   */
  @Query("DELETE FROM EmailLog l WHERE l.campaign.id = :campaignId")
  void deleteByCampaignId(@Param("campaignId") Long campaignId);
}
