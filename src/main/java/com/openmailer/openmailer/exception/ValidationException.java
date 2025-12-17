package com.openmailer.openmailer.exception;

/**
 * Exception thrown when business logic validation fails.
 * Results in HTTP 400 Bad Request response.
 */
public class ValidationException extends RuntimeException {
  private final String field;

  public ValidationException(String message) {
    super(message);
    this.field = null;
  }

  public ValidationException(String message, String field) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
