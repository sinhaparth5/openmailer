package com.openmailer.openmailer.exception;

import com.openmailer.openmailer.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for all REST API endpoints.
 * Converts exceptions to consistent ApiResponse format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handle ResourceNotFoundException (404)
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
      ResourceNotFoundException ex, WebRequest request) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage(), ex.getFieldName()));
  }

  /**
   * Handle UnauthorizedException (401)
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiResponse<Object>> handleUnauthorized(
      UnauthorizedException ex, WebRequest request) {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error("UNAUTHORIZED", ex.getMessage(), null));
  }

  /**
   * Handle ValidationException (400)
   */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidation(
      ValidationException ex, WebRequest request) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("VALIDATION_ERROR", ex.getMessage(), ex.getField()));
  }

  /**
   * Handle RateLimitExceededException (429)
   */
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ApiResponse<Object>> handleRateLimitExceeded(
      RateLimitExceededException ex, WebRequest request) {
    return ResponseEntity
        .status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", ex.getMessage(), null));
  }

  /**
   * Handle Spring validation errors (400)
   * Triggered by @Valid annotation on controller methods
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, WebRequest request) {
    // Return first error for simplicity
    FieldError firstError = (FieldError) ex.getBindingResult().getAllErrors().get(0);

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("VALIDATION_ERROR", firstError.getDefaultMessage(), firstError.getField()));
  }

  /**
   * Handle IllegalArgumentException (400)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("INVALID_ARGUMENT", ex.getMessage(), null));
  }

  /**
   * Handle missing static resources and unmapped asset requests as normal 404s.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(
      NoResourceFoundException ex, WebRequest request) {
    log.debug("Resource not found: {}", ex.getResourcePath());
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error("RESOURCE_NOT_FOUND", "The requested resource was not found.", ex.getResourcePath()));
  }

  /**
   * Handle all other exceptions (500)
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleGlobalException(
      Exception ex, WebRequest request) {
    log.error("Unhandled exception", ex);

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.", null));
  }
}
