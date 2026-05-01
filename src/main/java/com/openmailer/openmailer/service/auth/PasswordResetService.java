package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.UserRepository;
import com.openmailer.openmailer.service.security.PasswordEncoderService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
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
      @Value("${app.mail.from:}") String configuredMailFrom,
      @Value("${spring.mail.username:}") String mailUsername
  ) {
    this.userRepository = userRepository;
    this.passwordEncoderService = passwordEncoderService;
    this.mailSender = mailSender;
    this.appBaseUrl = appBaseUrl;
    this.mailFrom = resolveMailFrom(configuredMailFrom, mailUsername);
  }

  @CacheEvict(value = "users", allEntries = true)
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

  @CacheEvict(value = "users", allEntries = true)
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
        <!DOCTYPE html>
        <html lang="en">
          <body style="margin:0; padding:0; background:#f6f1e7; color:#171717; font-family:Arial,Helvetica,sans-serif;">
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f6f1e7; margin:0; padding:32px 0;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px; width:100%%; margin:0 auto; padding:0 16px;">
                    <tr>
                      <td style="padding:0 0 16px 0;">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#1a1a1a; border-radius:24px; overflow:hidden;">
                          <tr>
                            <td style="padding:22px 28px; border-bottom:1px solid rgba(255,255,255,0.08);">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td align="left" style="vertical-align:middle;">
                                    <table role="presentation" cellspacing="0" cellpadding="0">
                                      <tr>
                                        <td style="width:36px; height:36px; border-radius:10px; background:#ddf04e; color:#111111; "
                                            + "font-size:12px; font-weight:800; text-align:center; vertical-align:middle;">
                                          OM
                                        </td>
                                        <td style="padding-left:12px; color:#ffffff; font-size:18px; font-weight:700; letter-spacing:-0.02em;">
                                          OpenMailer
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                  <td align="right" style="color:#ffffff; font-size:11px; font-weight:700; letter-spacing:0.16em; text-transform:uppercase;">
                                    Security email
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:34px 28px 30px 28px;">
                              <div style="display:inline-block; padding:7px 12px; border-radius:999px; "
                                  + "background:rgba(221,240,78,0.12); color:#ddf04e; font-size:11px; "
                                  + "font-weight:700; letter-spacing:0.12em; text-transform:uppercase;">
                                Password reset
                              </div>
                              <h1 style="margin:18px 0 12px 0; color:#ffffff; font-size:32px; line-height:1.1; font-weight:800; letter-spacing:-0.03em;">
                                Reset your password
                              </h1>
                              <p style="margin:0 0 14px 0; color:rgba(255,255,255,0.74); font-size:15px; line-height:1.75;">
                                We received a request to reset the password for your OpenMailer account.
                              </p>
                              <p style="margin:0 0 24px 0; color:rgba(255,255,255,0.58); font-size:14px; line-height:1.75;">
                                If this was you, use the secure button below to choose a new password. This reset link will expire in %d minutes.
                              </p>
                              <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 0 24px 0;">
                                <tr>
                                  <td style="border-radius:999px; background:#ddf04e; text-align:center;">
                                    <a href="%s" style="display:inline-block; padding:14px 22px; color:#111111; text-decoration:none; font-size:14px; font-weight:800;">
                                      Choose a new password
                                    </a>
                                  </td>
                                </tr>
                              </table>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:0 0 24px 0; border-collapse:separate;">
                                <tr>
                                  <td style="padding:18px 20px; border-radius:18px; background:rgba(255,255,255,0.05); border:1px solid rgba(255,255,255,0.08);">
                                    <p style="margin:0 0 8px 0; color:#ffffff; font-size:13px; font-weight:700;">
                                      Didn’t request this?
                                    </p>
                                    <p style="margin:0; color:rgba(255,255,255,0.56); font-size:13px; line-height:1.7;">
                                      You can safely ignore this email. Your password will remain unchanged unless the reset link is used.
                                    </p>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0 0 10px 0; color:#ffffff; font-size:12px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase;">
                                Manual link
                              </p>
                              <p style="margin:0; color:rgba(255,255,255,0.58); font-size:12px; line-height:1.75; word-break:break-all;">
                                %s
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:8px 8px 0 8px; color:#6b665d; font-size:12px; line-height:1.7; text-align:center;">
                        This email was sent by OpenMailer regarding your account security.
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.formatted(RESET_TOKEN_EXPIRATION_MINUTES, resetUrl, resetUrl);

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

  private String resolveMailFrom(String configuredMailFrom, String mailUsername) {
    if (configuredMailFrom != null && !configuredMailFrom.isBlank()) {
      return configuredMailFrom.trim();
    }
    if (mailUsername != null && !mailUsername.isBlank()) {
      return mailUsername.trim();
    }
    return "no-reply@openmailer.local";
  }
}
