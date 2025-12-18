package com.openmailer.openmailer.service.email.provider;

import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.ProviderType;
import com.openmailer.openmailer.service.email.EmailSender;
import com.openmailer.openmailer.service.security.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating EmailSender instances based on provider type
 * Handles decryption of provider credentials and instantiation
 */
@Component
public class ProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(ProviderFactory.class);

    private final EncryptionService encryptionService;

    public ProviderFactory(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Creates an EmailSender instance for the given provider
     * Decrypts encrypted configuration values before instantiating
     *
     * @param provider The email provider entity
     * @return EmailSender instance configured for the provider
     * @throws IllegalArgumentException if provider type is unsupported or configuration is invalid
     */
    public EmailSender createProvider(EmailProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }

        if (!provider.isActive()) {
            throw new IllegalArgumentException("Provider is not active: " + provider.getName());
        }

        // Decrypt encrypted configuration values
        Map<String, String> decryptedConfig = decryptConfiguration(provider.getConfigurationMap());

        // Create a copy of the provider with decrypted configuration
        EmailProvider decryptedProvider = copyProviderWithDecryptedConfig(provider, decryptedConfig);

        // Instantiate the appropriate provider
        return switch (provider.getProviderType()) {
            case AWS_SES -> {
                log.info("Creating AWS SES provider: {}", provider.getName());
                yield new AwsSesProvider(decryptedProvider);
            }
            case SENDGRID -> {
                log.info("Creating SendGrid provider: {}", provider.getName());
                yield new SendGridProvider(decryptedProvider);
            }
            case SMTP -> {
                log.info("Creating SMTP provider: {}", provider.getName());
                yield new SmtpProvider(decryptedProvider);
            }
        };
    }

    /**
     * Decrypts encrypted configuration values
     * Assumes encrypted values are stored with "encrypted:" prefix
     *
     * @param configuration The configuration map
     * @return Map with decrypted values
     */
    private Map<String, String> decryptConfiguration(Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> decrypted = new HashMap<>();

        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Check if value needs decryption
            if (needsDecryption(key) && value != null && !value.isEmpty()) {
                try {
                    // Decrypt if the value is encrypted
                    if (encryptionService.isEncrypted(value)) {
                        String decryptedValue = encryptionService.decrypt(value);
                        decrypted.put(key, decryptedValue);
                        log.debug("Decrypted configuration key: {}", key);
                    } else {
                        // Value is not encrypted, use as-is
                        decrypted.put(key, value);
                        log.warn("Configuration key '{}' should be encrypted but isn't", key);
                    }
                } catch (Exception e) {
                    log.error("Failed to decrypt configuration key: {}", key, e);
                    throw new RuntimeException("Failed to decrypt provider configuration", e);
                }
            } else {
                // Non-sensitive value, use as-is
                decrypted.put(key, value);
            }
        }

        return decrypted;
    }

    /**
     * Determines if a configuration key contains sensitive data that should be encrypted
     *
     * @param key The configuration key
     * @return true if the key should be encrypted
     */
    private boolean needsDecryption(String key) {
        return key.equalsIgnoreCase("apiKey")
                || key.equalsIgnoreCase("accessKey")
                || key.equalsIgnoreCase("secretKey")
                || key.equalsIgnoreCase("password")
                || key.toLowerCase().contains("secret")
                || key.toLowerCase().contains("token");
    }

    /**
     * Creates a copy of the provider with decrypted configuration
     *
     * @param original The original provider
     * @param decryptedConfig The decrypted configuration
     * @return A new EmailProvider instance with decrypted config
     */
    private EmailProvider copyProviderWithDecryptedConfig(EmailProvider original, Map<String, String> decryptedConfig) {
        EmailProvider copy = new EmailProvider();
        copy.setId(original.getId());
        copy.setProviderName(original.getProviderName());
        copy.setProviderType(original.getProviderType());
        copy.setIsActive(original.getIsActive());
        copy.setDailyLimit(original.getDailyLimit());
        copy.setMonthlyLimit(original.getMonthlyLimit());
        copy.setUser(original.getUser());
        copy.setConfigurationMap(decryptedConfig);
        return copy;
    }

    /**
     * Validates that a provider is properly configured
     *
     * @param provider The email provider entity
     * @return true if the provider is valid and can be used
     */
    public boolean isProviderValid(EmailProvider provider) {
        if (provider == null || !provider.isActive()) {
            return false;
        }

        try {
            EmailSender sender = createProvider(provider);
            return sender.isConfigured();
        } catch (Exception e) {
            log.error("Provider validation failed for: {}", provider.getName(), e);
            return false;
        }
    }
}
