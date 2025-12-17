package com.openmailer.openmailer.service.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service class for password encoding and verification.
 * Uses BCrypt hashing algorithm for secure password storage.
 */
@Service
public class PasswordEncoderService {

  private final PasswordEncoder passwordEncoder;

  public PasswordEncoderService() {
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  /**
   * Encode a raw password.
   *
   * @param rawPassword the raw password
   * @return the encoded password
   */
  public String encode(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  /**
   * Verify if a raw password matches an encoded password.
   *
   * @param rawPassword the raw password
   * @param encodedPassword the encoded password
   * @return true if passwords match, false otherwise
   */
  public boolean matches(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
  }

  /**
   * Get the password encoder instance.
   *
   * @return the password encoder
   */
  public PasswordEncoder getPasswordEncoder() {
    return passwordEncoder;
  }
}
