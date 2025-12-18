package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.PaginatedResponse;
import com.openmailer.openmailer.model.Domain;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.domain.DomainService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for domain management
 */
@RestController
@RequestMapping("/api/v1/domains")
public class DomainController {

    private static final Logger log = LoggerFactory.getLogger(DomainController.class);

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    /**
     * GET /api/v1/domains - List all domains
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<Map<String, Object>>> listDomains(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Domain> domains = domainService.findByUserId(user.getId(), pageable);

        List<Map<String, Object>> responses = domains.getContent().stream()
                .map(this::domainToResponse)
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(
                page, size, domains.getTotalElements(), domains.getTotalPages()
        );

        return ResponseEntity.ok(new PaginatedResponse<>(responses, pagination));
    }

    /**
     * GET /api/v1/domains/{id} - Get domain by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDomain(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Domain domain = domainService.findById(id);

        // Check ownership
        if (!domain.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this domain", null));
        }

        return ResponseEntity.ok(ApiResponse.success(domainToResponse(domain)));
    }

    /**
     * POST /api/v1/domains - Add new domain
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDomain(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody Map<String, String> request) {

        String domainName = request.get("domainName");
        if (domainName == null || domainName.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "domainName is required", "domainName"));
        }

        // Check if domain already exists
        if (domainService.domainNameExists(domainName)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("DOMAIN_EXISTS", "Domain already exists", "domainName"));
        }

        Domain domain = new Domain();
        domain.setDomainName(domainName);
        domain.setStatus("PENDING");
        domain.setUser(user);

        // Generate DKIM keys
        // TODO: Implement DKIM key generation service
        // DkimKeyPair keys = dkimUtils.generateDkimKeys();
        // domain.setDkimPublicKey(keys.getPublicKey());
        // domain.setDkimPrivateKey(encryptionService.encrypt(keys.getPrivateKey()));

        Domain saved = domainService.createDomain(domain);

        log.info("Domain created: {} by user: {}", saved.getDomainName(), user.getEmail());

        Map<String, Object> response = domainToResponse(saved);
        response.put("dnsRecords", generateDnsRecords(saved));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Domain added. Please configure DNS records."));
    }

    /**
     * DELETE /api/v1/domains/{id} - Delete domain
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDomain(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Domain domain = domainService.findById(id);

        // Check ownership
        if (!domain.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this domain", null));
        }

        domainService.deleteDomain(id, user.getId());

        log.info("Domain deleted: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(null, "Domain deleted successfully"));
    }

    /**
     * GET /api/v1/domains/{id}/dns-records - Get DNS records for domain
     */
    @GetMapping("/{id}/dns-records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDnsRecords(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Domain domain = domainService.findById(id);

        // Check ownership
        if (!domain.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this domain", null));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("instructions", "Add these DNS records to your domain provider");
        result.put("records", generateDnsRecords(domain));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * POST /api/v1/domains/{id}/verify - Trigger manual domain verification
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyDomain(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Domain domain = domainService.findById(id);

        // Check ownership
        if (!domain.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this domain", null));
        }

        // TODO: Implement DNS verification service
        // DnsVerificationResult result = dnsVerificationService.verify(domain);
        // domain.setSpfVerified(result.isSpfVerified());
        // domain.setDkimVerified(result.isDkimVerified());
        // domain.setDmarcVerified(result.isDmarcVerified());

        // For now, simulate verification
        boolean allVerified = true; // TODO: Replace with actual verification
        boolean spfVerified = true;
        boolean dkimVerified = true;
        boolean dmarcVerified = true;

        String status;
        if (allVerified) {
            status = "VERIFIED";
            log.info("Domain verified: {} by user: {}", domain.getDomainName(), user.getEmail());
        } else {
            status = "FAILED";
            log.warn("Domain verification failed: {} by user: {}", domain.getDomainName(), user.getEmail());
        }

        Domain updated = domainService.updateVerificationStatus(id, user.getId(), status,
                spfVerified, dkimVerified, dmarcVerified);

        Map<String, Object> response = new HashMap<>();
        response.put("domainName", updated.getDomainName());
        response.put("status", updated.getStatus());
        response.put("spfVerified", updated.getSpfVerified());
        response.put("dkimVerified", updated.getDkimVerified());
        response.put("dmarcVerified", updated.getDmarcVerified());
        response.put("verifiedAt", updated.getVerifiedAt());

        return ResponseEntity.ok(ApiResponse.success(response,
                allVerified ? "Domain verified successfully" : "Domain verification failed"));
    }

    /**
     * Convert Domain entity to response map
     */
    private Map<String, Object> domainToResponse(Domain domain) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", domain.getId());
        response.put("domainName", domain.getDomainName());
        response.put("status", domain.getStatus());
        response.put("spfVerified", domain.getSpfVerified());
        response.put("dkimVerified", domain.getDkimVerified());
        response.put("dmarcVerified", domain.getDmarcVerified());
        response.put("verifiedAt", domain.getVerifiedAt());
        response.put("createdAt", domain.getCreatedAt());
        return response;
    }

    /**
     * Generate DNS records for domain
     */
    private List<Map<String, Object>> generateDnsRecords(Domain domain) {
        List<Map<String, Object>> records = new java.util.ArrayList<>();

        // SPF Record
        Map<String, Object> spf = new HashMap<>();
        spf.put("type", "TXT");
        spf.put("name", "@");
        spf.put("value", "v=spf1 include:openmailer.com ~all");
        spf.put("verified", domain.getSpfVerified());
        records.add(spf);

        // DKIM Record
        Map<String, Object> dkim = new HashMap<>();
        dkim.put("type", "TXT");
        dkim.put("name", "openmailer._domainkey");
        dkim.put("value", domain.getDkimPublicKey() != null ? "v=DKIM1; k=rsa; p=" + domain.getDkimPublicKey() : "Generating...");
        dkim.put("verified", domain.getDkimVerified());
        records.add(dkim);

        // DMARC Record
        Map<String, Object> dmarc = new HashMap<>();
        dmarc.put("type", "TXT");
        dmarc.put("name", "_dmarc");
        dmarc.put("value", "v=DMARC1; p=quarantine; rua=mailto:dmarc@openmailer.com");
        dmarc.put("verified", domain.getDmarcVerified());
        records.add(dmarc);

        return records;
    }
}
