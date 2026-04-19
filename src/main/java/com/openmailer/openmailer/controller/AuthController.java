package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.request.auth.ChangePasswordRequest;
import com.openmailer.openmailer.dto.request.auth.ForgotPasswordRequest;
import com.openmailer.openmailer.dto.request.auth.LoginRequest;
import com.openmailer.openmailer.dto.request.auth.LoginTwoFactorRequest;
import com.openmailer.openmailer.dto.request.auth.RegisterRequest;
import com.openmailer.openmailer.dto.request.auth.ResetPasswordRequest;
import com.openmailer.openmailer.dto.request.auth.UpdateProfileRequest;
import com.openmailer.openmailer.dto.request.twofa.TwoFactorVerifyRequest;
import com.openmailer.openmailer.dto.response.auth.LoginResponse;
import com.openmailer.openmailer.dto.ApiResponse;
import com.openmailer.openmailer.dto.response.twofa.TwoFactorBackupCodesResponse;
import com.openmailer.openmailer.dto.response.twofa.TwoFactorSetupResponse;
import com.openmailer.openmailer.dto.response.twofa.TwoFactorStatusResponse;
import com.openmailer.openmailer.config.JwtAuthenticationFilter;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.auth.AuthenticationService;
import com.openmailer.openmailer.service.auth.PasswordResetService;
import com.openmailer.openmailer.service.auth.TwoFactorAuthService;
import com.openmailer.openmailer.service.auth.UserService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, and token management.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final String REFRESH_TOKEN_COOKIE = "openmailer_refresh_token";
  private static final long REFRESH_TOKEN_MAX_AGE_SECONDS = TimeUnit.DAYS.toSeconds(7);
  private static final int MAX_PROFILE_IMAGE_BYTES = 2 * 1024 * 1024;

  private final AuthenticationService authenticationService;
  private final PasswordResetService passwordResetService;
  private final TwoFactorAuthService twoFactorAuthService;
  private final UserService userService;
  private final boolean secureCookies;
  private final String cookieDomain;

  @Autowired
  public AuthController(
      AuthenticationService authenticationService,
      PasswordResetService passwordResetService,
      TwoFactorAuthService twoFactorAuthService,
      UserService userService,
      @Value("${app.security.cookies.secure:false}") boolean secureCookies,
      @Value("${app.security.cookies.domain:}") String cookieDomain
  ) {
    this.authenticationService = authenticationService;
    this.passwordResetService = passwordResetService;
    this.twoFactorAuthService = twoFactorAuthService;
    this.userService = userService;
    this.secureCookies = secureCookies;
    this.cookieDomain = cookieDomain;
  }

  /**
   * Register a new user.
   *
   * @param request the registration request
   * @return the login response with tokens
   */
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<LoginResponse>> register(
      @Valid @RequestBody RegisterRequest request,
      HttpServletResponse servletResponse
  ) {
    LoginResponse loginResponse = authenticationService.register(request);
    attachAuthCookies(servletResponse, loginResponse, true);
    return ResponseEntity.ok(ApiResponse.success(loginResponse));
  }

  /**
   * Authenticate user and generate tokens.
   *
   * @param request the login request
   * @return the login response with tokens
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletResponse servletResponse
  ) {
    LoginResponse response = authenticationService.login(request);
    if (!Boolean.TRUE.equals(response.getRequiresTwoFactor())) {
      attachAuthCookies(servletResponse, response, Boolean.TRUE.equals(request.getRememberMe()));
    }
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping("/login/2fa")
  public ResponseEntity<ApiResponse<LoginResponse>> verifyLoginTwoFactor(
      @Valid @RequestBody LoginTwoFactorRequest request,
      HttpServletResponse servletResponse
  ) {
    LoginResponse response = authenticationService.verifyTwoFactorLogin(request);
    attachAuthCookies(servletResponse, response, Boolean.TRUE.equals(request.getRememberMe()));
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Refresh access token using refresh token.
   *
   * @param refreshToken the refresh token
   * @return the new login response with tokens
   */
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
      @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
      @RequestBody(required = false) String refreshToken,
      HttpServletResponse servletResponse
  ) {
    String token = refreshTokenCookie != null && !refreshTokenCookie.isBlank() ? refreshTokenCookie : refreshToken;
    if (token == null || token.isBlank()) {
      throw new com.openmailer.openmailer.exception.UnauthorizedException("Refresh token is required");
    }

    LoginResponse response = authenticationService.refreshToken(token);
    attachAuthCookies(servletResponse, response, true);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Get current authenticated user.
   *
   * @param userDetails the authenticated user details
   * @return the current user
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(ApiResponse.success(authenticationService.buildUserInfo(userDetails.getUser())));
  }

  @PutMapping("/me")
  public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> updateCurrentUser(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateProfileRequest request
  ) {
    User updatedUser = new User();
    updatedUser.setUsername(request.getUsername().trim());
    updatedUser.setEmail(request.getEmail().trim());
    updatedUser.setFirstName(blankToNull(request.getFirstName()));
    updatedUser.setLastName(blankToNull(request.getLastName()));

    User savedUser = authenticationService.updateProfile(userDetails.getUser().getId(), updatedUser);
    return ResponseEntity.ok(ApiResponse.success(authenticationService.buildUserInfo(savedUser), "Profile updated successfully."));
  }

  @PostMapping("/change-password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ChangePasswordRequest request
  ) {
    authenticationService.changePassword(
        userDetails.getUser().getId(),
        request.getCurrentPassword(),
        request.getNewPassword()
    );
    return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully."));
  }

  @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> uploadAvatar(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam("file") MultipartFile file
  ) throws java.io.IOException {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_FILE", "Choose an image to upload.", "file"));
    }

    String contentType = file.getContentType();
    if (contentType == null || !isAllowedProfileImageContentType(contentType)) {
      return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
          .body(ApiResponse.error("INVALID_FILE_TYPE", "Only PNG, JPEG, GIF, or WEBP images are allowed.", "file"));
    }

    if (file.getSize() > MAX_PROFILE_IMAGE_BYTES) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("FILE_TOO_LARGE", "Profile images must be 2MB or smaller.", "file"));
    }

    User savedUser = userService.updateProfileImage(
        userDetails.getUser().getId(),
        file.getBytes(),
        contentType,
        (int) file.getSize()
    );
    return ResponseEntity.ok(ApiResponse.success(authenticationService.buildUserInfo(savedUser), "Profile picture updated successfully."));
  }

  @DeleteMapping("/me/avatar")
  public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> deleteAvatar(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    User savedUser = userService.removeProfileImage(userDetails.getUser().getId());
    return ResponseEntity.ok(ApiResponse.success(authenticationService.buildUserInfo(savedUser), "Profile picture removed successfully."));
  }

  /**
   * Logout user (client-side token removal).
   *
   * @return success message
   */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
    clearAuthCookies(response);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.requestPasswordReset(request.getEmail());
    return ResponseEntity.ok(ApiResponse.success(
        null,
        "If an account exists for that email, a reset link has been sent."
    ));
  }

  @GetMapping("/reset-password/validate")
  public ResponseEntity<ApiResponse<Boolean>> validateResetToken(@RequestParam("token") String token) {
    return ResponseEntity.ok(ApiResponse.success(passwordResetService.isResetTokenValid(token)));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.getToken(), request.getPassword());
    return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully."));
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

  @GetMapping("/2fa/status")
  public ResponseEntity<ApiResponse<TwoFactorStatusResponse>> getTwoFactorStatus(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    User user = userDetails.getUser();
    TwoFactorStatusResponse response = new TwoFactorStatusResponse(
        twoFactorAuthService.isTwoFactorEnabled(user.getId()),
        twoFactorAuthService.getRemainingBackupCodes(user.getId()).size()
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

  private void attachAuthCookies(HttpServletResponse servletResponse, LoginResponse loginResponse, boolean rememberMe) {
    long accessTokenMaxAge = loginResponse.getExpiresIn() != null ? loginResponse.getExpiresIn() : 3600;
    ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, loginResponse.getAccessToken())
        .httpOnly(true)
        .secure(secureCookies)
        .path("/")
        .sameSite("Lax");

    ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from(REFRESH_TOKEN_COOKIE, loginResponse.getRefreshToken())
        .httpOnly(true)
        .secure(secureCookies)
        .path("/api/auth")
        .sameSite("Strict");

    if (cookieDomain != null && !cookieDomain.isBlank()) {
      accessCookieBuilder.domain(cookieDomain);
      refreshCookieBuilder.domain(cookieDomain);
    }

    if (rememberMe) {
      accessCookieBuilder.maxAge(accessTokenMaxAge);
      refreshCookieBuilder.maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS);
    }

    ResponseCookie accessCookie = accessCookieBuilder.build();
    ResponseCookie refreshCookie = refreshCookieBuilder.build();

    servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
  }

  private void clearAuthCookies(HttpServletResponse servletResponse) {
    ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(secureCookies)
        .path("/")
        .sameSite("Lax")
        .maxAge(0);

    ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
        .httpOnly(true)
        .secure(secureCookies)
        .path("/api/auth")
        .sameSite("Strict")
        .maxAge(0);

    if (cookieDomain != null && !cookieDomain.isBlank()) {
      accessCookieBuilder.domain(cookieDomain);
      refreshCookieBuilder.domain(cookieDomain);
    }

    ResponseCookie accessCookie = accessCookieBuilder.build();
    ResponseCookie refreshCookie = refreshCookieBuilder.build();

    servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private boolean isAllowedProfileImageContentType(String contentType) {
    return MediaType.IMAGE_PNG_VALUE.equals(contentType)
        || MediaType.IMAGE_JPEG_VALUE.equals(contentType)
        || MediaType.IMAGE_GIF_VALUE.equals(contentType)
        || "image/webp".equals(contentType);
  }
}
