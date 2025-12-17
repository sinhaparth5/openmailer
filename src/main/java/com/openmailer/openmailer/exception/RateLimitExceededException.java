package com.openmailer.openmailer.exception;

/**
 * Exception thrown when rate limit is exceeded.
 * Results in HTTP 429 Too Many Requests response.
 */
public class RateLimitExceededException extends RuntimeException {
  private final int limit;
  private final long retryAfterSeconds;

  public RateLimitExceededException(String message, int limit, long retryAfterSeconds) {
    super(message);
    this.limit = limit;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public int getLimit() {
    return limit;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
