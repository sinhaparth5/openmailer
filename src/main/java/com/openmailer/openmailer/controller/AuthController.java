package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.dto.request.auth.LoginRequest;
import com.openmailer.openmailer.dto.request.auth.RegisterRequest;
import com.openmailer.openmailer.dto.response.auth.LoginResponse;
import com.openmailer.openmailer.dto.response.common.ApiResponse;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.auth.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, and token management.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  @Autowired
  public AuthController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
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
}
