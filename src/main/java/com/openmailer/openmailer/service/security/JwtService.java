package com.openmailer.openmailer.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service class for JWT token management.
 * Handles token generation, validation, and parsing.
 */
@Service
public class JwtService {

  @Value("${jwt.secret:openmailer-secret-key-change-this-in-production-to-a-secure-random-value}")
  private String secret;

  @Value("${jwt.access-token-expiration:3600000}") // 1 hour in milliseconds
  private Long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration:604800000}") // 7 days in milliseconds
  private Long refreshTokenExpiration;

  /**
   * Generate access token for user.
   *
   * @param userId the user ID
   * @param email the user email
   * @return the generated access token
   */
  public String generateAccessToken(Long userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("email", email);
    return generateToken(claims, email, accessTokenExpiration);
  }

  /**
   * Generate refresh token for user.
   *
   * @param userId the user ID
   * @param email the user email
   * @return the generated refresh token
   */
  public String generateRefreshToken(Long userId, String email) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("email", email);
    return generateToken(claims, email, refreshTokenExpiration);
  }

  /**
   * Generate token with custom claims.
   *
   * @param claims the claims
   * @param subject the subject (usually email)
   * @param expiration the expiration time in milliseconds
   * @return the generated token
   */
  public String generateToken(Map<String, Object> claims, String subject, Long expiration) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Validate token.
   *
   * @param token the token
   * @param email the user email
   * @return true if valid, false otherwise
   */
  public boolean validateToken(String token, String email) {
    final String tokenEmail = extractEmail(token);
    return (tokenEmail.equals(email) && !isTokenExpired(token));
  }

  /**
   * Extract email from token.
   *
   * @param token the token
   * @return the email
   */
  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extract user ID from token.
   *
   * @param token the token
   * @return the user ID
   */
  public Long extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", Long.class));
  }

  /**
   * Extract expiration date from token.
   *
   * @param token the token
   * @return the expiration date
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Extract a specific claim from token.
   *
   * @param token the token
   * @param claimsResolver the claims resolver function
   * @param <T> the type of the claim
   * @return the claim value
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Extract all claims from token.
   *
   * @param token the token
   * @return the claims
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Check if token is expired.
   *
   * @param token the token
   * @return true if expired, false otherwise
   */
  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Get signing key for JWT.
   *
   * @return the signing key
   */
  private SecretKey getSigningKey() {
    byte[] keyBytes = secret.getBytes();
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
