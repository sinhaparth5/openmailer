package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.request.auth.LoginRequest;
import com.openmailer.openmailer.dto.request.auth.RegisterRequest;
import com.openmailer.openmailer.dto.request.twofa.TwoFactorVerifyRequest;
import com.openmailer.openmailer.dto.response.auth.LoginResponse;
import com.openmailer.openmailer.dto.response.common.ApiResponse;
import com.openmailer.openmailer.dto.response.twofa.TwoFactorBackupCodesResponse;
import com.openmailer.openmailer.dto.response.twofa.TwoFactorSetupResponse;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.auth.AuthenticationService;
import com.openmailer.openmailer.service.auth.TwoFactorAuthService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, and token management.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationService authenticationService;
  private final TwoFactorAuthService twoFactorAuthService;

  @Autowired
  public AuthController(AuthenticationService authenticationService, TwoFactorAuthService twoFactorAuthService) {
    this.authenticationService = authenticationService;
    this.twoFactorAuthService = twoFactorAuthService;
  }

  /**
   * Register a new user.
   *
   * @param request the registration request
   * @return the login response with tokens
   */
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
    LoginResponse response = authenticationService.register(request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Authenticate user and generate tokens.
   *
   * @param request the login request
   * @return the login response with tokens
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authenticationService.login(request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Refresh access token using refresh token.
   *
   * @param refreshToken the refresh token
   * @return the new login response with tokens
   */
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody String refreshToken) {
    LoginResponse response = authenticationService.refreshToken(refreshToken);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Get current authenticated user.
   *
   * @param userDetails the authenticated user details
   * @return the current user
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
    User user = userDetails.getUser();
    return ResponseEntity.ok(ApiResponse.success(user));
  }

  /**
   * Logout user (client-side token removal).
   *
   * @return success message
   */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout() {
    // JWT is stateless, so logout is handled client-side by removing the token
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  /**
   * Setup 2FA for the current user.
   * Generates a secret and QR code for scanning with authenticator app.
   *
   * @param userDetails the authenticated user
   * @return secret and QR code data URL
   */
  @PostMapping("/2fa/setup")
  public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setupTwoFactor(
          @AuthenticationPrincipal CustomUserDetails userDetails) {

    User user = userDetails.getUser();

    try {
      // Generate secret
      String secret = twoFactorAuthService.generateSecret(user.getId());

      // Generate QR code
      String qrCodeDataUrl = twoFactorAuthService.generateQrCodeDataUrl(user.getId(), secret);

      TwoFactorSetupResponse response = new TwoFactorSetupResponse(
              secret,
              qrCodeDataUrl,
              "Scan the QR code with your authenticator app and verify with a code to enable 2FA"
      );

      return ResponseEntity.ok(ApiResponse.success(response));

    } catch (QrGenerationException e) {
      throw new RuntimeException("Failed to generate QR code", e);
    }
  }

  /**
   * Enable 2FA after verifying the setup code.
   *
   * @param userDetails the authenticated user
   * @param request the verification code
   * @return backup codes
   */
  @PostMapping("/2fa/enable")
  public ResponseEntity<ApiResponse<TwoFactorBackupCodesResponse>> enableTwoFactor(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody TwoFactorVerifyRequest request) {

    User user = userDetails.getUser();

    // Enable 2FA and get backup codes
    List<String> backupCodes = twoFactorAuthService.enableTwoFactor(user.getId(), request.getCode());

    TwoFactorBackupCodesResponse response = new TwoFactorBackupCodesResponse(
            backupCodes,
            "Two-factor authentication enabled successfully. Save these backup codes in a secure place."
    );

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Disable 2FA for the current user.
   *
   * @param userDetails the authenticated user
   * @return success message
   */
  @PostMapping("/2fa/disable")
  public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
          @AuthenticationPrincipal CustomUserDetails userDetails) {

    User user = userDetails.getUser();
    twoFactorAuthService.disableTwoFactor(user.getId());

    return ResponseEntity.ok(ApiResponse.success(null));
  }

  /**
   * Regenerate backup codes for 2FA.
   *
   * @param userDetails the authenticated user
   * @return new backup codes
   */
  @PostMapping("/2fa/backup-codes")
  public ResponseEntity<ApiResponse<TwoFactorBackupCodesResponse>> regenerateBackupCodes(
          @AuthenticationPrincipal CustomUserDetails userDetails) {

    User user = userDetails.getUser();
    List<String> backupCodes = twoFactorAuthService.regenerateBackupCodes(user.getId());

    TwoFactorBackupCodesResponse response = new TwoFactorBackupCodesResponse(
            backupCodes,
            "New backup codes generated successfully. Save these in a secure place."
    );

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Verify a 2FA code (for login or testing).
   *
   * @param userDetails the authenticated user
   * @param request the verification code
   * @return success status
   */
  @PostMapping("/2fa/verify")
  public ResponseEntity<ApiResponse<Boolean>> verifyTwoFactorCode(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody TwoFactorVerifyRequest request) {

    User user = userDetails.getUser();
    boolean isValid = twoFactorAuthService.verifyCode(user.getId(), request.getCode());

    return ResponseEntity.ok(ApiResponse.success(isValid));
  }
}
