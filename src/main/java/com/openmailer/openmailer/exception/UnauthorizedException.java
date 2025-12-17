package com.openmailer.openmailer.exception;

/**
 * Exception thrown when authentication or authorization fails.
 * Results in HTTP 401 Unauthorized response.
 */
public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
