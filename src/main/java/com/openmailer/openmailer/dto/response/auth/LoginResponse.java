package com.openmailer.openmailer.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for successful login.
 * Contains JWT tokens and user information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
  private String accessToken;
  private String refreshToken;
  private String tokenType = "Bearer";
  private Integer expiresIn;
  private Boolean requiresTwoFactor;
  private String pendingTwoFactorToken;
  private UserInfo user;

  public LoginResponse() {
  }

  public LoginResponse(String accessToken, String refreshToken, Integer expiresIn, UserInfo user) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
    this.user = user;
  }

  public static LoginResponse pendingTwoFactor(String pendingTwoFactorToken, UserInfo user) {
    LoginResponse response = new LoginResponse();
    response.setRequiresTwoFactor(true);
    response.setPendingTwoFactorToken(pendingTwoFactorToken);
    response.setUser(user);
    return response;
  }

  // Nested UserInfo class
  public static class UserInfo {
    private String id;
    private String email;
    private String username;
    private String role;
    private String firstName;
    private String lastName;
    private Boolean twoFactorEnabled;
    private String accountStatus;

    public UserInfo() {
    }

    public UserInfo(
        String id,
        String email,
        String username,
        String role,
        String firstName,
        String lastName,
        Boolean twoFactorEnabled,
        String accountStatus
    ) {
      this.id = id;
      this.email = email;
      this.username = username;
      this.role = role;
      this.firstName = firstName;
      this.lastName = lastName;
      this.twoFactorEnabled = twoFactorEnabled;
      this.accountStatus = accountStatus;
    }

    // Getters and Setters
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public Boolean getTwoFactorEnabled() {
      return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
      this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getAccountStatus() {
      return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
      this.accountStatus = accountStatus;
    }
  }

  // Getters and Setters
  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public Integer getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
  }

  public Boolean getRequiresTwoFactor() {
    return requiresTwoFactor;
  }

  public void setRequiresTwoFactor(Boolean requiresTwoFactor) {
    this.requiresTwoFactor = requiresTwoFactor;
  }

  public String getPendingTwoFactorToken() {
    return pendingTwoFactorToken;
  }

  public void setPendingTwoFactorToken(String pendingTwoFactorToken) {
    this.pendingTwoFactorToken = pendingTwoFactorToken;
  }

  public UserInfo getUser() {
    return user;
  }

  public void setUser(UserInfo user) {
    this.user = user;
  }
}
