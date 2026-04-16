package com.openmailer.openmailer.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginTwoFactorRequest {

  @NotBlank(message = "Pending two-factor token is required")
  private String pendingTwoFactorToken;

  @NotBlank(message = "Verification code is required")
  @Pattern(regexp = "^[0-9]{6}$|^[A-Z0-9]{8}$", message = "Code must be either a 6-digit authenticator code or 8-character backup code")
  private String code;

  private Boolean rememberMe = false;

  public String getPendingTwoFactorToken() {
    return pendingTwoFactorToken;
  }

  public void setPendingTwoFactorToken(String pendingTwoFactorToken) {
    this.pendingTwoFactorToken = pendingTwoFactorToken;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Boolean getRememberMe() {
    return rememberMe;
  }

  public void setRememberMe(Boolean rememberMe) {
    this.rememberMe = rememberMe;
  }
}
