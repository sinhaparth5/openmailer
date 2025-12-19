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
  private Integer expiresIn;
  private UserInfo user;

  public LoginResponse() {
  }

  public LoginResponse(String accessToken, String refreshToken, Integer expiresIn, UserInfo user) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
    this.user = user;
  }

  // Nested UserInfo class
  public static class UserInfo {
    private String id;
    private String email;
    private String username;
    private String role;

    public UserInfo() {
    }

    public UserInfo(String id, String email, String username, String role) {
      this.id = id;
      this.email = email;
      this.username = username;
      this.role = role;
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

  public Integer getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
  }

  public UserInfo getUser() {
    return user;
  }

  public void setUser(UserInfo user) {
    this.user = user;
  }
}
