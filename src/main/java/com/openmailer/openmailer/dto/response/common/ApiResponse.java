package com.openmailer.openmailer.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Generic wrapper for all API responses.
 * Provides consistent response structure across all endpoints.
 *
 * @param <T> Type of data being returned
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  private boolean success;
  private T data;
  private ErrorResponse error;
  private LocalDateTime timestamp;

  public ApiResponse() {
    this.timestamp = LocalDateTime.now();
  }

  public ApiResponse(boolean success, T data) {
    this.success = success;
    this.data = data;
    this.timestamp = LocalDateTime.now();
  }

  public ApiResponse(boolean success, T data, ErrorResponse error) {
    this.success = success;
    this.data = data;
    this.error = error;
    this.timestamp = LocalDateTime.now();
  }

  // Static factory methods for cleaner API
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data);
  }

  public static <T> ApiResponse<T> error(ErrorResponse error) {
    return new ApiResponse<>(false, null, error);
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    return new ApiResponse<>(false, null, new ErrorResponse(code, message, null));
  }

  // Getters and Setters
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public ErrorResponse getError() {
    return error;
  }

  public void setError(ErrorResponse error) {
    this.error = error;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }
}
