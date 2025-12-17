package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.EmailCampaign;
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
 * Repository interface for EmailCampaign entity.
 * Provides CRUD operations and custom query methods for email campaigns.
 */
@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {

  /**
   * Find all campaigns for a specific user.
   *
   * @param userId the user ID
   * @return list of email campaigns
   */
  List<EmailCampaign> findByUserId(Long userId);

  /**
   * Find all campaigns for a specific user with pagination.
   *
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of email campaigns
   */
  Page<EmailCampaign> findByUserId(Long userId, Pageable pageable);

  /**
   * Find campaign by ID and user ID.
   *
   * @param id the campaign ID
   * @param userId the user ID
   * @return Optional containing the campaign if found
   */
  Optional<EmailCampaign> findByIdAndUserId(Long id, Long userId);

  /**
   * Find campaigns by status.
   *
   * @param userId the user ID
   * @param status the campaign status
   * @param pageable pagination information
   * @return page of campaigns
   */
  Page<EmailCampaign> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

  /**
   * Find campaigns by name containing search term.
   *
   * @param userId the user ID
   * @param name the search term
   * @param pageable pagination information
   * @return page of matching campaigns
   */
  Page<EmailCampaign> findByUserIdAndNameContainingIgnoreCase(Long userId, String name, Pageable pageable);

  /**
   * Find scheduled campaigns that are ready to send.
   *
   * @param status the scheduled status
   * @param now current timestamp
   * @return list of campaigns ready to send
   */
  @Query("SELECT c FROM EmailCampaign c WHERE c.status = :status AND c.scheduledAt <= :now")
  List<EmailCampaign> findScheduledCampaignsReadyToSend(@Param("status") String status, @Param("now") LocalDateTime now);

  /**
   * Find campaigns by template ID.
   *
   * @param templateId the template ID
   * @return list of campaigns using this template
   */
  List<EmailCampaign> findByTemplateId(Long templateId);

  /**
   * Count campaigns for a specific user.
   *
   * @param userId the user ID
   * @return count of campaigns
   */
  long countByUserId(Long userId);

  /**
   * Count campaigns by status for a user.
   *
   * @param userId the user ID
   * @param status the campaign status
   * @return count of campaigns with given status
   */
  long countByUserIdAndStatus(Long userId, String status);

  /**
   * Delete all campaigns for a specific user.
   *
   * @param userId the user ID
   */
  void deleteByUserId(Long userId);
}
