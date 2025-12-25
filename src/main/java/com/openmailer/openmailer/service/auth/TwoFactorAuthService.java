package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.UserRepository;
import com.openmailer.openmailer.service.security.EncryptionService;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Two-Factor Authentication (2FA) operations.
 * Handles TOTP generation, QR code creation, code verification, and backup codes.
 */
@Service
@Transactional
public class TwoFactorAuthService {

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final DefaultSecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier verifier;

    @Autowired
    public TwoFactorAuthService(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();

        // Create code verifier with default settings
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    /**
     * Generate a new TOTP secret for a user.
     *
     * @param userId the user ID
     * @return the base32-encoded secret
     */
    public String generateSecret(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        // Generate new secret
        String secret = secretGenerator.generate();

        // Encrypt and store the secret (but don't enable 2FA yet)
        String encryptedSecret = encryptionService.encrypt(secret);
        user.setTwoFactorSecret(encryptedSecret);
        userRepository.save(user);

        return secret;
    }

    /**
     * Generate QR code image data URL for setting up 2FA.
     *
     * @param userId the user ID
     * @param secret the TOTP secret
     * @return data URL for QR code image
     * @throws QrGenerationException if QR generation fails
     */
    public String generateQrCodeDataUrl(String userId, String secret) throws QrGenerationException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("OpenMailer")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        // Generate QR code as byte array
        byte[] imageData = qrGenerator.generate(data);

        // Convert to base64 data URI
        String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);
        return "data:image/png;base64," + base64Image;
    }

    /**
     * Verify a TOTP code against the user's secret.
     *
     * @param userId the user ID
     * @param code the 6-digit code to verify
     * @return true if code is valid, false otherwise
     */
    public boolean verifyCode(String userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        if (user.getTwoFactorSecret() == null) {
            return false;
        }

        // Decrypt the secret
        String secret = encryptionService.decrypt(user.getTwoFactorSecret());

        // Verify the code
        return verifier.isValidCode(secret, code);
    }

    /**
     * Verify a backup code.
     *
     * @param userId the user ID
     * @param code the backup code to verify
     * @return true if backup code is valid and removed, false otherwise
     */
    public boolean verifyBackupCode(String userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        if (user.getTwoFactorBackupCodes() == null || user.getTwoFactorBackupCodes().isBlank()) {
            return false;
        }

        // Decrypt backup codes
        String decryptedCodes = encryptionService.decrypt(user.getTwoFactorBackupCodes());
        List<String> backupCodes = new ArrayList<>(Arrays.asList(decryptedCodes.split(",")));

        // Check if code exists
        if (!backupCodes.contains(code)) {
            return false;
        }

        // Remove used backup code
        backupCodes.remove(code);

        // Encrypt and save remaining codes
        if (backupCodes.isEmpty()) {
            user.setTwoFactorBackupCodes(null);
        } else {
            String updatedCodes = String.join(",", backupCodes);
            user.setTwoFactorBackupCodes(encryptionService.encrypt(updatedCodes));
        }

        userRepository.save(user);
        return true;
    }

    /**
     * Enable 2FA for a user after verifying the setup code.
     *
     * @param userId the user ID
     * @param code the verification code
     * @return list of backup codes
     */
    public List<String> enableTwoFactor(String userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        // Verify the code first
        if (!verifyCode(userId, code)) {
            throw new ValidationException("Invalid verification code", "code");
        }

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();

        // Encrypt and store backup codes
        String encryptedCodes = encryptionService.encrypt(String.join(",", backupCodes));
        user.setTwoFactorBackupCodes(encryptedCodes);

        // Enable 2FA
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        return backupCodes;
    }

    /**
     * Disable 2FA for a user.
     *
     * @param userId the user ID
     */
    public void disableTwoFactor(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorBackupCodes(null);
        userRepository.save(user);
    }

    /**
     * Generate new backup codes for a user.
     *
     * @param userId the user ID
     * @return list of new backup codes
     */
    public List<String> regenerateBackupCodes(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        if (!user.getTwoFactorEnabled()) {
            throw new ValidationException("Two-factor authentication is not enabled", "twoFactorEnabled");
        }

        // Generate new backup codes
        List<String> backupCodes = generateBackupCodes();

        // Encrypt and store
        String encryptedCodes = encryptionService.encrypt(String.join(",", backupCodes));
        user.setTwoFactorBackupCodes(encryptedCodes);
        userRepository.save(user);

        return backupCodes;
    }

    /**
     * Get remaining backup codes for a user.
     *
     * @param userId the user ID
     * @return list of remaining backup codes
     */
    public List<String> getRemainingBackupCodes(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        if (user.getTwoFactorBackupCodes() == null || user.getTwoFactorBackupCodes().isBlank()) {
            return new ArrayList<>();
        }

        String decryptedCodes = encryptionService.decrypt(user.getTwoFactorBackupCodes());
        return Arrays.asList(decryptedCodes.split(","));
    }

    /**
     * Generate a list of random backup codes.
     *
     * @return list of 10 backup codes (8 characters each)
     */
    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            // Generate 8-character alphanumeric code
            String code = random.ints(8, 0, 36)
                    .mapToObj(n -> n < 10 ? String.valueOf(n) : String.valueOf((char) ('A' + n - 10)))
                    .collect(Collectors.joining());
            codes.add(code);
        }

        return codes;
    }

    /**
     * Check if user has 2FA enabled.
     *
     * @param userId the user ID
     * @return true if 2FA is enabled
     */
    public boolean isTwoFactorEnabled(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", "userId"));

        return user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled();
    }
}
