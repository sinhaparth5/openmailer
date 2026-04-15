package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.UserRepository;
import com.openmailer.openmailer.service.security.PasswordEncoderService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
  private static final int RESET_TOKEN_BYTES = 32;
  private static final long RESET_TOKEN_EXPIRATION_MINUTES = 30;

  private final UserRepository userRepository;
  private final PasswordEncoderService passwordEncoderService;
  private final JavaMailSender mailSender;
  private final String appBaseUrl;
  private final String mailFrom;
  private final SecureRandom secureRandom = new SecureRandom();

  public PasswordResetService(
      UserRepository userRepository,
      PasswordEncoderService passwordEncoderService,
      JavaMailSender mailSender,
      @Value("${app.base-url:http://localhost:8080}") String appBaseUrl,
      @Value("${spring.mail.username:no-reply@openmailer.local}") String mailFrom
  ) {
    this.userRepository = userRepository;
    this.passwordEncoderService = passwordEncoderService;
    this.mailSender = mailSender;
    this.appBaseUrl = appBaseUrl;
    this.mailFrom = mailFrom;
  }

  public void requestPasswordReset(String email) {
    Optional<User> optionalUser = userRepository.findByEmail(email);
    if (optionalUser.isEmpty()) {
      return;
    }

    User user = optionalUser.get();
    String rawToken = generateToken();
    user.setPasswordResetTokenHash(hashToken(rawToken));
    user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES));
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    sendResetEmail(user, rawToken);
  }

  @Transactional(readOnly = true)
  public boolean isResetTokenValid(String token) {
    return findUserByResetToken(token).isPresent();
  }

  public void resetPassword(String token, String newPassword) {
    User user = findUserByResetToken(token)
        .orElseThrow(() -> new ValidationException("Reset link is invalid or has expired", "token"));

    user.setPassword(passwordEncoderService.encode(newPassword));
    user.setPasswordResetTokenHash(null);
    user.setPasswordResetTokenExpiresAt(null);
    user.setFailedLoginAttempts(0);
    user.setAccountLockedUntil(null);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
  }

  private Optional<User> findUserByResetToken(String token) {
    String tokenHash = hashToken(token);
    return userRepository.findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(
        tokenHash,
        LocalDateTime.now()
    );
  }

  private String generateToken() {
    byte[] bytes = new byte[RESET_TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }

  private void sendResetEmail(User user, String rawToken) {
    String resetUrl = appBaseUrl + "/reset-password?token=" + rawToken;
    String subject = "Reset your OpenMailer password";
    String htmlBody = """
        <html>
          <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #1f2937;">
            <h2 style="margin-bottom: 12px;">Reset your password</h2>
            <p>We received a request to reset the password for your OpenMailer account.</p>
            <p>
              <a href="%s" style="display: inline-block; padding: 12px 18px; background: #171717; color: #ffffff; text-decoration: none; border-radius: 999px; font-weight: 600;">
                Choose a new password
              </a>
            </p>
            <p>This link expires in %d minutes.</p>
            <p>If you did not request this, you can ignore this email.</p>
            <p style="font-size: 12px; color: #6b7280;">If the button does not work, use this URL: %s</p>
          </body>
        </html>
        """.formatted(resetUrl, RESET_TOKEN_EXPIRATION_MINUTES, resetUrl);

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setTo(user.getEmail());
      helper.setFrom(mailFrom);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(message);
    } catch (MessagingException ex) {
      log.error("Failed to send password reset email to {}", user.getEmail(), ex);
      throw new IllegalStateException("Failed to send password reset email", ex);
    }
  }
}
