package com.openmailer.openmailer.exception;

import com.openmailer.openmailer.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler.
 * Returns JSON for API/AJAX requests and error pages for normal browser navigation.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public Object handleResourceNotFound(
      ResourceNotFoundException ex, WebRequest request) {
    if (wantsJson(request)) {
      return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage(), ex.getFieldName()));
    }
    return errorPage(HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public Object handleUnauthorized(
      UnauthorizedException ex, WebRequest request) {
    if (wantsJson(request)) {
      return ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", ex.getMessage(), null));
    }
    return errorPage(HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(ValidationException.class)
  public Object handleValidation(
      ValidationException ex, WebRequest request) {
    if (wantsJson(request)) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("VALIDATION_ERROR", ex.getMessage(), ex.getField()));
    }
    return errorPage(HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public Object handleRateLimitExceeded(
      RateLimitExceededException ex, WebRequest request) {
    if (wantsJson(request)) {
      return ResponseEntity
          .status(HttpStatus.TOO_MANY_REQUESTS)
          .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
          .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", ex.getMessage(), null));
    }
    return errorPage(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Object handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, WebRequest request) {
    if (wantsJson(request)) {
      FieldError firstError = (FieldError) ex.getBindingResult().getAllErrors().get(0);
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("VALIDATION_ERROR", firstError.getDefaultMessage(), firstError.getField()));
    }
    return errorPage(HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public Object handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request) {
    if (wantsJson(request)) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("INVALID_ARGUMENT", ex.getMessage(), null));
    }
    return errorPage(HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public Object handleNoResourceFound(
      NoResourceFoundException ex, WebRequest request) {
    log.debug("Resource not found: {}", ex.getResourcePath());
    if (wantsJson(request)) {
      return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("RESOURCE_NOT_FOUND", "The requested resource was not found.", ex.getResourcePath()));
    }
    return errorPage(HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public Object handleGlobalException(
      Exception ex, WebRequest request) {
    log.error("Unhandled exception", ex);

    if (wantsJson(request)) {
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.", null));
    }

    return errorPage(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private boolean wantsJson(WebRequest request) {
    if (!(request instanceof ServletWebRequest servletWebRequest)) {
      return true;
    }

    HttpServletRequest httpRequest = servletWebRequest.getRequest();
    String uri = httpRequest.getRequestURI();
    String accept = httpRequest.getHeader(HttpHeaders.ACCEPT);
    String requestedWith = httpRequest.getHeader("X-Requested-With");

    if (uri != null && (uri.startsWith("/api/") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui"))) {
      return true;
    }

    if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
      return true;
    }

    return accept != null && accept.contains("application/json");
  }

  private ModelAndView errorPage(HttpStatus status) {
    ModelAndView modelAndView = new ModelAndView("error/" + status.value());
    modelAndView.setStatus(status);
    modelAndView.addObject("status", status.value());
    modelAndView.addObject("error", status.getReasonPhrase());
    return modelAndView;
  }
}
