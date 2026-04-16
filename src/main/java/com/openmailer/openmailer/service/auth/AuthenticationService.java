package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.dto.request.auth.LoginRequest;
import com.openmailer.openmailer.dto.request.auth.LoginTwoFactorRequest;
import com.openmailer.openmailer.dto.request.auth.RegisterRequest;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.dto.response.auth.LoginResponse;
import com.openmailer.openmailer.exception.UnauthorizedException;
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

  private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
  private static final long ACCOUNT_LOCK_MINUTES = 15;
  private static final String PENDING_TWO_FACTOR_TOKEN_TYPE = "pending-2fa";

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
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoderService.encode(request.getPassword()));
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setEnabled(true);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    user = userService.createUser(user);

    String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    LoginResponse.UserInfo userInfo = buildUserInfo(user);

    return new LoginResponse(accessToken, refreshToken, 3600, userInfo);
  }

  public User updateProfile(String userId, User updatedUser) {
    return userService.updateUser(userId, updatedUser);
  }

  /**
   * Authenticate user and generate tokens.
   * First-step login using email and password.
   *
   * @param request the login request
   * @return the login response with tokens
   * @throws UnauthorizedException if credentials are invalid or 2FA code is required/invalid
   */
  public LoginResponse login(LoginRequest request) {
    User user = userService.findByEmail(request.getEmail())
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

    if (isAccountLocked(user)) {
      throw new UnauthorizedException("Account is locked. Try again later");
    }

    // Verify password
    if (!passwordEncoderService.matches(request.getPassword(), user.getPassword())) {
      recordFailedLoginAttempt(user);
      throw new UnauthorizedException("Invalid email or password");
    }

    // Check if user is enabled
    if (!user.getEnabled()) {
      throw new UnauthorizedException("Account is disabled");
    }

    if (user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled()) {
      return LoginResponse.pendingTwoFactor(
          jwtService.generatePendingTwoFactorToken(user.getId(), user.getEmail(), user.getUsername(), user.getRole()),
          buildUserInfo(user)
      );
    }

    return completeLogin(user);
  }

  public LoginResponse verifyTwoFactorLogin(LoginTwoFactorRequest request) {
    try {
      String pendingToken = request.getPendingTwoFactorToken();
      String email = jwtService.extractEmail(pendingToken);
      User user = userService.findByEmailOrThrow(email);

      if (!jwtService.validateToken(pendingToken, user.getEmail(), PENDING_TWO_FACTOR_TOKEN_TYPE)) {
        throw new UnauthorizedException("Two-factor verification session has expired. Sign in again.");
      }

      if (isAccountLocked(user)) {
        throw new UnauthorizedException("Account is locked. Try again later");
      }

      boolean isValidTotp = twoFactorAuthService.verifyCode(user.getId(), request.getCode());
      boolean isValidBackup = false;

      if (!isValidTotp && request.getCode() != null && request.getCode().length() == 8) {
        isValidBackup = twoFactorAuthService.verifyBackupCode(user.getId(), request.getCode());
      }

      if (!isValidTotp && !isValidBackup) {
        recordFailedLoginAttempt(user);
        throw new UnauthorizedException("Invalid two-factor authentication code");
      }

      return completeLogin(user);
    } catch (UnauthorizedException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new UnauthorizedException("Two-factor verification session has expired. Sign in again.");
    }
  }

  private LoginResponse completeLogin(User user) {
    userService.recordSuccessfulLogin(user.getId(), LocalDateTime.now());

    String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    LoginResponse.UserInfo userInfo = buildUserInfo(user);

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

      if (!jwtService.validateToken(refreshToken, user.getEmail(), "refresh")) {
        throw new UnauthorizedException("Invalid refresh token");
      }

      String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
      String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
      LoginResponse.UserInfo userInfo = buildUserInfo(user);

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

      if (!jwtService.validateToken(accessToken, user.getEmail(), "access")) {
        throw new UnauthorizedException("Invalid access token");
      }

      return user;
    } catch (Exception e) {
      throw new UnauthorizedException("Invalid access token");
    }
  }

  public LoginResponse.UserInfo buildUserInfo(User user) {
    return new LoginResponse.UserInfo(
        user.getId(),
        user.getEmail(),
        user.getUsername(),
        user.getRole(),
        user.getFirstName(),
        user.getLastName(),
        user.getTwoFactorEnabled(),
        user.getAccountStatus()
    );
  }

  public void changePassword(String userId, String currentPassword, String newPassword) {
    User user = userService.findById(userId);

    if (!passwordEncoderService.matches(currentPassword, user.getPassword())) {
      throw new ValidationException("Current password is incorrect", "currentPassword");
    }

    if (passwordEncoderService.matches(newPassword, user.getPassword())) {
      throw new ValidationException("New password must be different from the current password", "newPassword");
    }

    user.setPassword(passwordEncoderService.encode(newPassword));
    user.setUpdatedAt(LocalDateTime.now());
    userService.updatePassword(user.getId(), user.getPassword());
  }

  private boolean isAccountLocked(User user) {
    return user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now());
  }

  private void recordFailedLoginAttempt(User user) {
    int nextFailedAttempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
    LocalDateTime lockUntil = nextFailedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS
        ? LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES)
        : null;
    userService.recordFailedLoginAttempt(user.getId(), lockUntil);
  }
}
