package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.dto.request.auth.LoginRequest;
import com.openmailer.openmailer.dto.request.auth.RegisterRequest;
import com.openmailer.openmailer.dto.response.auth.LoginResponse;
import com.openmailer.openmailer.exception.UnauthorizedException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.security.JwtService;
import com.openmailer.openmailer.service.security.PasswordEncoderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service class for authentication operations.
 * Handles user registration, login, and token management.
 */
@Service
@Transactional
public class AuthenticationService {

  private final UserService userService;
  private final PasswordEncoderService passwordEncoderService;
  private final JwtService jwtService;
  private final TwoFactorAuthService twoFactorAuthService;

  @Autowired
  public AuthenticationService(
      UserService userService,
      PasswordEncoderService passwordEncoderService,
      JwtService jwtService,
      TwoFactorAuthService twoFactorAuthService
  ) {
    this.userService = userService;
    this.passwordEncoderService = passwordEncoderService;
    this.jwtService = jwtService;
    this.twoFactorAuthService = twoFactorAuthService;
  }

  /**
   * Register a new user.
   *
   * @param request the registration request
   * @return the login response with tokens
   * @throws ValidationException if email already exists
   */
  public LoginResponse register(RegisterRequest request) {
    // Check if email already exists
    if (userService.emailExists(request.getEmail())) {
      throw new ValidationException("Email already exists", "email");
    }

    // Create new user
    User user = new User();
    user.setUsername(request.getEmail()); // Use email as username
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoderService.encode(request.getPassword()));
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setEnabled(true);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    user = userService.createUser(user);

    // Generate tokens
    String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
    String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

    // Create user info
    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
        user.getId(),
        user.getEmail(),
        user.getUsername(),
        user.getRole()
    );

    return new LoginResponse(accessToken, refreshToken, 3600, userInfo);
  }

  /**
   * Authenticate user and generate tokens.
   * Supports two-factor authentication if enabled.
   *
   * @param request the login request
   * @return the login response with tokens
   * @throws UnauthorizedException if credentials are invalid or 2FA code is required/invalid
   */
  public LoginResponse login(LoginRequest request) {
    // Find user by email
    User user = userService.findByEmailOrThrow(request.getEmail());

    // Verify password
    if (!passwordEncoderService.matches(request.getPassword(), user.getPassword())) {
      throw new UnauthorizedException("Invalid email or password");
    }

    // Check if user is enabled
    if (!user.getEnabled()) {
      throw new UnauthorizedException("Account is disabled");
    }

    // Check 2FA if enabled
    if (user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled()) {
      if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isBlank()) {
        throw new UnauthorizedException("Two-factor authentication code is required");
      }

      // Verify 2FA code (try both TOTP and backup code)
      boolean isValidTotp = twoFactorAuthService.verifyCode(user.getId(), request.getTwoFactorCode());
      boolean isValidBackup = false;

      if (!isValidTotp) {
        // Check if it's a backup code (8 characters)
        if (request.getTwoFactorCode().length() == 8) {
          isValidBackup = twoFactorAuthService.verifyBackupCode(user.getId(), request.getTwoFactorCode());
        }
      }

      if (!isValidTotp && !isValidBackup) {
        throw new UnauthorizedException("Invalid two-factor authentication code");
      }
    }

    // Update last login
    user.setLastLoginAt(LocalDateTime.now());
    userService.updateUser(user.getId(), user);

    // Generate tokens
    String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
    String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

    // Create user info
    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
        user.getId(),
        user.getEmail(),
        user.getUsername(),
        user.getRole()
    );

    return new LoginResponse(accessToken, refreshToken, 3600, userInfo);
  }

  /**
   * Refresh access token using refresh token.
   *
   * @param refreshToken the refresh token
   * @return the new login response with tokens
   * @throws UnauthorizedException if refresh token is invalid
   */
  public LoginResponse refreshToken(String refreshToken) {
    try {
      // Extract email from refresh token
      String email = jwtService.extractEmail(refreshToken);

      // Find user
      User user = userService.findByEmailOrThrow(email);

      // Validate refresh token
      if (!jwtService.validateToken(refreshToken, user.getEmail())) {
        throw new UnauthorizedException("Invalid refresh token");
      }

      // Generate new tokens
      String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
      String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

      // Create user info
      LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
          user.getId(),
          user.getEmail(),
          user.getUsername(),
          user.getRole()
      );

      return new LoginResponse(newAccessToken, newRefreshToken, 3600, userInfo);
    } catch (Exception e) {
      throw new UnauthorizedException("Invalid refresh token");
    }
  }

  /**
   * Validate access token and return user.
   *
   * @param accessToken the access token
   * @return the user
   * @throws UnauthorizedException if token is invalid
   */
  @Transactional(readOnly = true)
  public User validateAccessToken(String accessToken) {
    try {
      String email = jwtService.extractEmail(accessToken);
      User user = userService.findByEmailOrThrow(email);

      if (!jwtService.validateToken(accessToken, user.getEmail())) {
        throw new UnauthorizedException("Invalid access token");
      }

      return user;
    } catch (Exception e) {
      throw new UnauthorizedException("Invalid access token");
    }
  }
}
