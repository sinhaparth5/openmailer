package com.openmailer.openmailer.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

  private static final String DEFAULT_SECRET = "openmailer-secret-key-change-this-in-production-to-a-secure-random-value";
  private static final String TOKEN_TYPE_ACCESS = "access";
  private static final String TOKEN_TYPE_REFRESH = "refresh";
  private static final String TOKEN_TYPE_PENDING_2FA = "pending-2fa";
  private static final long PENDING_TWO_FACTOR_EXPIRATION = 5 * 60 * 1000L;

  private final String secret;

  private final Long accessTokenExpiration;

  private final Long refreshTokenExpiration;

  public JwtService(
      @Value("${jwt.secret:}") String secret,
      @Value("${jwt.access-token-expiration:3600000}") Long accessTokenExpiration,
      @Value("${jwt.refresh-token-expiration:604800000}") Long refreshTokenExpiration,
      Environment environment) {
    boolean prodProfile = environment.matchesProfiles("prod");
    if (secret == null || secret.isBlank()) {
      if (prodProfile) {
        throw new IllegalStateException("JWT_SECRET must be configured in production.");
      }
      secret = DEFAULT_SECRET;
    }

    if (prodProfile && DEFAULT_SECRET.equals(secret)) {
      throw new IllegalStateException("JWT_SECRET cannot use the development default in production.");
    }

    if (secret.getBytes().length < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 32 bytes.");
    }

    this.secret = secret;
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  /**
   * Generate access token for user.
   *
   * @param userId the ID (String)
   * @param email the user email
   * @return the generated access token
   */
  public String generateAccessToken(String userId, String email, String username, String role) {
    return generateToken(buildClaims(userId, email, username, role, TOKEN_TYPE_ACCESS), email, accessTokenExpiration);
  }

  /**
   * Generate refresh token for user.
   *
   * @param userId the ID (String)
   * @param email the user email
   * @return the generated refresh token
   */
  public String generateRefreshToken(String userId, String email, String username, String role) {
    return generateToken(buildClaims(userId, email, username, role, TOKEN_TYPE_REFRESH), email, refreshTokenExpiration);
  }

  public String generatePendingTwoFactorToken(String userId, String email, String username, String role) {
    return generateToken(buildClaims(userId, email, username, role, TOKEN_TYPE_PENDING_2FA), email, PENDING_TWO_FACTOR_EXPIRATION);
  }

  /**
   * Generate token with custom claims.
   *
   * @param claims the claims
   * @param subject the subject (usually email)
   * @param expiration the expiration time in milliseconds
   * @return the generated token
   */
  public String generateToken(Map<String, Object> claims, String subject, long expiration) {
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

  public boolean validateToken(String token, String email, String expectedTokenType) {
    return validateToken(token, email) && expectedTokenType.equals(extractTokenType(token));
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
  public String extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", String.class));
  }

  public String extractUsername(String token) {
    return extractClaim(token, claims -> claims.get("username", String.class));
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  public String extractTokenType(String token) {
    return extractClaim(token, claims -> claims.get("tokenType", String.class));
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

  private Map<String, Object> buildClaims(String userId, String email, String username, String role, String tokenType) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("email", email);
    claims.put("username", username);
    claims.put("role", role);
    claims.put("tokenType", tokenType);
    return claims;
  }
}
