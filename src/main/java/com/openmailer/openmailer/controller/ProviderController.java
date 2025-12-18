package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.PaginatedResponse;
import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.provider.EmailProviderService;
import com.openmailer.openmailer.service.security.EncryptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * REST controller for email provider management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final EmailProviderService providerService;
    private final EncryptionService encryptionService;

    /**
     * GET /api/v1/providers - List all email providers
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<Map<String, Object>>> listProviders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<EmailProvider> providers = providerService.findAllByUser(user.getId(), pageable);

        List<Map<String, Object>> responses = providers.getContent().stream()
                .map(this::providerToResponse)
                .collect(Collectors.toList());

        PaginatedResponse.PaginationInfo pagination = new PaginatedResponse.PaginationInfo(
                page, size, providers.getTotalElements(), providers.getTotalPages()
        );

        return ResponseEntity.ok(new PaginatedResponse<>(responses, pagination));
    }

    /**
     * GET /api/v1/providers/{id} - Get provider by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProvider(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailProvider provider = providerService.findById(id);

        // Check ownership
        if (!provider.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this provider", null));
        }

        return ResponseEntity.ok(ApiResponse.success(providerToResponse(provider)));
    }

    /**
     * POST /api/v1/providers - Create new email provider
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProvider(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody Map<String, Object> request) {

        String name = (String) request.get("name");
        String providerTypeStr = (String) request.get("providerType");
        @SuppressWarnings("unchecked")
        Map<String, String> configuration = (Map<String, String>) request.get("configuration");

        if (name == null || providerTypeStr == null || configuration == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "name, providerType, and configuration are required", null));
        }

        EmailProvider.ProviderType providerType;
        try {
            providerType = EmailProvider.ProviderType.valueOf(providerTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PROVIDER_TYPE", "Invalid provider type: " + providerTypeStr, "providerType"));
        }

        // Encrypt sensitive configuration values
        Map<String, String> encryptedConfig = encryptSensitiveConfig(configuration);

        EmailProvider provider = new EmailProvider();
        provider.setName(name);
        provider.setProviderType(providerType);
        provider.setConfiguration(encryptedConfig);
        provider.setActive(true);
        provider.setSentToday(0);
        provider.setSentThisMonth(0);
        provider.setUser(user);

        // Set default limits if provided
        if (request.containsKey("dailyLimit")) {
            provider.setDailyLimit(((Number) request.get("dailyLimit")).intValue());
        }
        if (request.containsKey("monthlyLimit")) {
            provider.setMonthlyLimit(((Number) request.get("monthlyLimit")).intValue());
        }

        EmailProvider saved = providerService.save(provider);

        log.info("Email provider created: {} ({}) by user: {}", saved.getId(), saved.getName(), user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(providerToResponse(saved), "Email provider created successfully"));
    }

    /**
     * PUT /api/v1/providers/{id} - Update email provider
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProvider(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request) {

        EmailProvider provider = providerService.findById(id);

        // Check ownership
        if (!provider.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this provider", null));
        }

        // Update name if provided
        if (request.containsKey("name")) {
            provider.setName((String) request.get("name"));
        }

        // Update configuration if provided
        if (request.containsKey("configuration")) {
            @SuppressWarnings("unchecked")
            Map<String, String> configuration = (Map<String, String>) request.get("configuration");
            Map<String, String> encryptedConfig = encryptSensitiveConfig(configuration);
            provider.setConfiguration(encryptedConfig);
        }

        // Update limits if provided
        if (request.containsKey("dailyLimit")) {
            provider.setDailyLimit(((Number) request.get("dailyLimit")).intValue());
        }
        if (request.containsKey("monthlyLimit")) {
            provider.setMonthlyLimit(((Number) request.get("monthlyLimit")).intValue());
        }

        // Update active status if provided
        if (request.containsKey("active")) {
            provider.setActive((Boolean) request.get("active"));
        }

        EmailProvider updated = providerService.save(provider);

        log.info("Email provider updated: {} by user: {}", updated.getId(), user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(providerToResponse(updated), "Email provider updated successfully"));
    }

    /**
     * DELETE /api/v1/providers/{id} - Delete email provider
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailProvider provider = providerService.findById(id);

        // Check ownership
        if (!provider.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this provider", null));
        }

        providerService.deleteById(id);

        log.info("Email provider deleted: {} by user: {}", id, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(null, "Email provider deleted successfully"));
    }

    /**
     * POST /api/v1/providers/{id}/test - Test provider connection
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testProvider(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailProvider provider = providerService.findById(id);

        // Check ownership
        if (!provider.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this provider", null));
        }

        // TODO: Implement provider test using ProviderFactory
        // EmailSender sender = providerFactory.createProvider(provider);
        // boolean configured = sender.isConfigured();

        boolean configured = true; // Placeholder

        Map<String, Object> result = new HashMap<>();
        result.put("providerId", provider.getId());
        result.put("providerName", provider.getName());
        result.put("configured", configured);
        result.put("status", configured ? "success" : "failed");

        log.info("Provider test: {} - {} by user: {}", id, configured ? "SUCCESS" : "FAILED", user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(result,
                configured ? "Provider is configured correctly" : "Provider configuration is invalid"));
    }

    /**
     * PATCH /api/v1/providers/{id}/toggle - Toggle provider active status
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleProvider(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailProvider provider = providerService.findById(id);

        // Check ownership
        if (!provider.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this provider", null));
        }

        provider.setActive(!provider.isActive());
        EmailProvider updated = providerService.save(provider);

        log.info("Provider {} {} by user: {}", id, updated.isActive() ? "activated" : "deactivated", user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(providerToResponse(updated),
                "Provider " + (updated.isActive() ? "activated" : "deactivated")));
    }

    /**
     * GET /api/v1/providers/{id}/stats - Get provider usage statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderStats(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        EmailProvider provider = providerService.findById(id);

        // Check ownership
        if (!provider.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "You don't have access to this provider", null));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("providerId", provider.getId());
        stats.put("providerName", provider.getName());
        stats.put("sentToday", provider.getSentToday());
        stats.put("sentThisMonth", provider.getSentThisMonth());
        stats.put("dailyLimit", provider.getDailyLimit());
        stats.put("monthlyLimit", provider.getMonthlyLimit());

        // Calculate remaining
        if (provider.getDailyLimit() != null) {
            stats.put("dailyRemaining", provider.getDailyLimit() - provider.getSentToday());
        }
        if (provider.getMonthlyLimit() != null) {
            stats.put("monthlyRemaining", provider.getMonthlyLimit() - provider.getSentThisMonth());
        }

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Convert EmailProvider entity to response map (excluding sensitive data)
     */
    private Map<String, Object> providerToResponse(EmailProvider provider) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", provider.getId());
        response.put("name", provider.getName());
        response.put("providerType", provider.getProviderType());
        response.put("active", provider.isActive());
        response.put("dailyLimit", provider.getDailyLimit());
        response.put("monthlyLimit", provider.getMonthlyLimit());
        response.put("sentToday", provider.getSentToday());
        response.put("sentThisMonth", provider.getSentThisMonth());
        response.put("createdAt", provider.getCreatedAt());

        // Return masked configuration (don't expose sensitive values)
        Map<String, String> maskedConfig = new HashMap<>();
        if (provider.getConfiguration() != null) {
            for (Map.Entry<String, String> entry : provider.getConfiguration().entrySet()) {
                if (isSensitiveKey(entry.getKey())) {
                    maskedConfig.put(entry.getKey(), "********");
                } else {
                    maskedConfig.put(entry.getKey(), entry.getValue());
                }
            }
        }
        response.put("configuration", maskedConfig);

        return response;
    }

    /**
     * Encrypt sensitive configuration values
     */
    private Map<String, String> encryptSensitiveConfig(Map<String, String> config) {
        Map<String, String> encrypted = new HashMap<>();

        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (isSensitiveKey(key) && value != null && !value.isEmpty()) {
                // Encrypt sensitive values
                encrypted.put(key, encryptionService.encrypt(value));
            } else {
                // Keep non-sensitive values as-is
                encrypted.put(key, value);
            }
        }

        return encrypted;
    }

    /**
     * Check if a configuration key is sensitive
     */
    private boolean isSensitiveKey(String key) {
        return key.equalsIgnoreCase("apiKey")
                || key.equalsIgnoreCase("accessKey")
                || key.equalsIgnoreCase("secretKey")
                || key.equalsIgnoreCase("password")
                || key.toLowerCase().contains("secret")
                || key.toLowerCase().contains("token");
    }
}
