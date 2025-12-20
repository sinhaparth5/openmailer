package com.openmailer.openmailer.service.domain;

import com.openmailer.openmailer.model.Domain;
import com.openmailer.openmailer.repository.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for verifying domain DNS records.
 * Runs hourly to check pending domains for SPF, DKIM, and DMARC verification.
 */
@Service
@Transactional
public class DomainVerificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DomainVerificationScheduler.class);

    private final DomainRepository domainRepository;
    private final DnsVerificationService dnsVerificationService;

    @Autowired
    public DomainVerificationScheduler(
            DomainRepository domainRepository,
            DnsVerificationService dnsVerificationService) {
        this.domainRepository = domainRepository;
        this.dnsVerificationService = dnsVerificationService;
    }

    /**
     * Scheduled task to verify pending domains every hour.
     * Runs at the top of every hour.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at 0 minutes
    public void verifyPendingDomains() {
        log.info("Starting scheduled domain verification");

        try {
            // Find domains that are pending verification
            List<Domain> pendingDomains = domainRepository.findByStatus("PENDING");

            if (pendingDomains.isEmpty()) {
                log.debug("No pending domains to verify");
                return;
            }

            log.info("Found {} pending domains to verify", pendingDomains.size());

            int verifiedCount = 0;
            int failedCount = 0;

            for (Domain domain : pendingDomains) {
                try {
                    boolean verified = verifyDomain(domain);
                    if (verified) {
                        verifiedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error verifying domain {}: {}",
                            domain.getDomainName(), e.getMessage(), e);
                    failedCount++;
                }
            }

            log.info("Domain verification completed. Verified: {}, Failed: {}",
                    verifiedCount, failedCount);

        } catch (Exception e) {
            log.error("Error in scheduled domain verification: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifies a single domain.
     *
     * @param domain the domain to verify
     * @return true if successfully verified, false otherwise
     */
    private boolean verifyDomain(Domain domain) {
        log.info("Verifying domain: {}", domain.getDomainName());

        try {
            // Get expected DKIM public key from domain record
            String dkimPublicKey = domain.getDkimPublicKey();
            String dkimSelector = domain.getDkimSelector() != null ? domain.getDkimSelector() : "openmailer";

            // Perform DNS verification
            DnsVerificationService.DnsVerificationResult result =
                    dnsVerificationService.verifyDomain(domain.getDomainName(), dkimSelector, dkimPublicKey);

            // Update domain status based on results
            domain.setSpfVerified(result.isSpfVerified());
            domain.setDkimVerified(result.isDkimVerified());
            domain.setDmarcVerified(result.isDmarcVerified());
            domain.setLastCheckedAt(LocalDateTime.now());

            if (result.isAllVerified()) {
                domain.setStatus("VERIFIED");
                domain.setVerifiedAt(LocalDateTime.now());
                log.info("Domain {} fully verified (SPF, DKIM, DMARC)", domain.getDomainName());
            } else {
                // Partially verified or still pending
                if (result.isSpfVerified() || result.isDkimVerified() || result.isDmarcVerified()) {
                    domain.setStatus("PARTIAL");
                    log.warn("Domain {} partially verified. SPF: {}, DKIM: {}, DMARC: {}",
                            domain.getDomainName(), result.isSpfVerified(),
                            result.isDkimVerified(), result.isDmarcVerified());
                } else {
                    // Check how long it's been pending
                    if (domain.getCreatedAt() != null &&
                        domain.getCreatedAt().plusDays(7).isBefore(LocalDateTime.now())) {
                        // After 7 days of no verification, mark as failed
                        domain.setStatus("FAILED");
                        log.warn("Domain {} failed verification after 7 days", domain.getDomainName());
                    } else {
                        // Keep as pending
                        log.info("Domain {} still pending verification", domain.getDomainName());
                    }
                }
            }

            domainRepository.save(domain);

            return result.isAllVerified();

        } catch (Exception e) {
            log.error("Failed to verify domain {}: {}", domain.getDomainName(), e.getMessage(), e);

            // Mark as failed after persistent errors
            domain.setStatus("ERROR");
            domain.setLastCheckedAt(LocalDateTime.now());
            domainRepository.save(domain);

            return false;
        }
    }

    /**
     * Re-verifies all domains regardless of status.
     * Useful for periodic re-verification of already verified domains.
     */
    @Scheduled(cron = "0 0 0 * * SUN") // Every Sunday at midnight
    public void reverifyAllDomains() {
        log.info("Starting weekly re-verification of all domains");

        try {
            List<Domain> allDomains = domainRepository.findAll();

            log.info("Re-verifying {} domains", allDomains.size());

            for (Domain domain : allDomains) {
                try {
                    verifyDomain(domain);
                } catch (Exception e) {
                    log.error("Error re-verifying domain {}: {}",
                            domain.getDomainName(), e.getMessage());
                }
            }

            log.info("Weekly domain re-verification completed");

        } catch (Exception e) {
            log.error("Error in weekly domain re-verification: {}", e.getMessage(), e);
        }
    }

    /**
     * Manually triggers domain verification for a specific domain.
     *
     * @param domainId the domain ID to verify
     * @return verification result
     */
    public DnsVerificationService.DnsVerificationResult verifyDomainById(String domainId) {
        log.info("Manual verification requested for domain: {}", domainId);

        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found: " + domainId));

        String dkimSelector = domain.getDkimSelector() != null ? domain.getDkimSelector() : "openmailer";
        DnsVerificationService.DnsVerificationResult result =
                dnsVerificationService.verifyDomain(domain.getDomainName(), dkimSelector, domain.getDkimPublicKey());

        // Update domain
        domain.setSpfVerified(result.isSpfVerified());
        domain.setDkimVerified(result.isDkimVerified());
        domain.setDmarcVerified(result.isDmarcVerified());
        domain.setLastCheckedAt(LocalDateTime.now());

        if (result.isAllVerified()) {
            domain.setStatus("VERIFIED");
            domain.setVerifiedAt(LocalDateTime.now());
        } else if (result.isSpfVerified() || result.isDkimVerified() || result.isDmarcVerified()) {
            domain.setStatus("PARTIAL");
        } else {
            domain.setStatus("FAILED");
        }

        domainRepository.save(domain);

        return result;
    }

    /**
     * Manually triggers verification of all pending domains.
     * Useful for testing or manual execution.
     */
    public void triggerManualVerification() {
        log.info("Manually triggering domain verification");
        verifyPendingDomains();
    }
}
