package com.openmailer.openmailer.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response structure for API errors.
 * Used within ApiResponse when success = false.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
  private String code;
  private String message;
  private String field;

  public ErrorResponse() {
  }

  public ErrorResponse(String code, String message, String field) {
    this.code = code;
    this.message = message;
    this.field = field;
  }

  // Static factory methods
  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(code, message, null);
  }

  public static ErrorResponse of(String code, String message, String field) {
    return new ErrorResponse(code, message, field);
  }

  // Getters and Setters
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }
}
