package com.openmailer.openmailer.exception;

import com.openmailer.openmailer.dto.response.common.ApiResponse;
import com.openmailer.openmailer.dto.response.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST API endpoints.
 * Converts exceptions to consistent ApiResponse format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle ResourceNotFoundException (404)
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
      ResourceNotFoundException ex, WebRequest request) {
    ErrorResponse error = ErrorResponse.of(
        "RESOURCE_NOT_FOUND",
        ex.getMessage(),
        ex.getFieldName()
    );
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(error));
  }

  /**
   * Handle UnauthorizedException (401)
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiResponse<Object>> handleUnauthorized(
      UnauthorizedException ex, WebRequest request) {
    ErrorResponse error = ErrorResponse.of(
        "UNAUTHORIZED",
        ex.getMessage()
    );
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(error));
  }

  /**
   * Handle ValidationException (400)
   */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidation(
      ValidationException ex, WebRequest request) {
    ErrorResponse error = ErrorResponse.of(
        "VALIDATION_ERROR",
        ex.getMessage(),
        ex.getField()
    );
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(error));
  }

  /**
   * Handle RateLimitExceededException (429)
   */
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ApiResponse<Object>> handleRateLimitExceeded(
      RateLimitExceededException ex, WebRequest request) {
    ErrorResponse error = ErrorResponse.of(
        "RATE_LIMIT_EXCEEDED",
        ex.getMessage()
    );
    return ResponseEntity
        .status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(ApiResponse.error(error));
  }

  /**
   * Handle Spring validation errors (400)
   * Triggered by @Valid annotation on controller methods
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, WebRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    // Return first error for simplicity
    FieldError firstError = (FieldError) ex.getBindingResult().getAllErrors().get(0);
    ErrorResponse error = ErrorResponse.of(
        "VALIDATION_ERROR",
        firstError.getDefaultMessage(),
        firstError.getField()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(error));
  }

  /**
   * Handle IllegalArgumentException (400)
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request) {
    ErrorResponse error = ErrorResponse.of(
        "INVALID_ARGUMENT",
        ex.getMessage()
    );
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(error));
  }

  /**
   * Handle all other exceptions (500)
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleGlobalException(
      Exception ex, WebRequest request) {
    ErrorResponse error = ErrorResponse.of(
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please try again later."
    );

    // Log the actual exception for debugging (in production, use proper logging)
    ex.printStackTrace();

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(error));
  }
}
