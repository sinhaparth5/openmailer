package com.openmailer.openmailer.service.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Service for generating and managing DKIM (DomainKeys Identified Mail) keys.
 * DKIM keys are RSA key pairs used to digitally sign outgoing emails.
 */
@Service
public class DkimKeyGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DkimKeyGenerationService.class);
    private static final int KEY_SIZE = 2048; // RSA key size in bits

    /**
     * Generates a new DKIM RSA key pair.
     *
     * @return DkimKeyPair containing the public and private keys
     * @throws RuntimeException if key generation fails
     */
    public DkimKeyPair generateDkimKeys() {
        try {
            log.info("Generating new DKIM RSA key pair (size: {} bits)", KEY_SIZE);

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            // Convert keys to Base64-encoded strings for storage
            String publicKey = encodePublicKey(keyPair.getPublic());
            String privateKey = encodePrivateKey(keyPair.getPrivate());

            log.info("DKIM key pair generated successfully");

            return new DkimKeyPair(publicKey, privateKey);

        } catch (NoSuchAlgorithmException e) {
            log.error("RSA algorithm not available for DKIM key generation", e);
            throw new RuntimeException("Failed to generate DKIM keys: RSA algorithm not available", e);
        } catch (Exception e) {
            log.error("Unexpected error during DKIM key generation", e);
            throw new RuntimeException("Failed to generate DKIM keys", e);
        }
    }

    /**
     * Encodes a public key to Base64 format for DNS TXT record.
     * Returns the key in the format suitable for DKIM DNS records (without headers).
     *
     * @param publicKey the public key
     * @return Base64-encoded public key string
     */
    private String encodePublicKey(PublicKey publicKey) {
        byte[] keyBytes = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * Encodes a private key to Base64 format for secure storage.
     * This key should be encrypted before storing in the database.
     *
     * @param privateKey the private key
     * @return Base64-encoded private key string
     */
    private String encodePrivateKey(PrivateKey privateKey) {
        byte[] keyBytes = privateKey.getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * Decodes a Base64-encoded public key string back to a PublicKey object.
     *
     * @param encodedKey the Base64-encoded public key
     * @return PublicKey object
     * @throws Exception if decoding fails
     */
    public PublicKey decodePublicKey(String encodedKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    /**
     * Decodes a Base64-encoded private key string back to a PrivateKey object.
     *
     * @param encodedKey the Base64-encoded private key
     * @return PrivateKey object
     * @throws Exception if decoding fails
     */
    public PrivateKey decodePrivateKey(String encodedKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Formats a public key for use in a DKIM DNS TXT record.
     * Removes line breaks and formats according to DKIM specification.
     *
     * @param publicKey the Base64-encoded public key
     * @return formatted public key for DNS record
     */
    public String formatPublicKeyForDns(String publicKey) {
        // Remove any whitespace or line breaks
        return publicKey.replaceAll("\\s+", "");
    }

    /**
     * Data class to hold a DKIM key pair.
     */
    public static class DkimKeyPair {
        private final String publicKey;
        private final String privateKey;

        public DkimKeyPair(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }
    }
}
