package com.openmailer.openmailer.config;

import com.openmailer.openmailer.service.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter.
 * Intercepts requests and validates JWT tokens.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String ACCESS_TOKEN_COOKIE = "openmailer_access_token";

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Autowired
  public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    final String jwt = extractToken(request);
    final String userEmail;

    if (jwt == null || jwt.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // Extract user email from JWT
      userEmail = jwtService.extractEmail(jwt);

      // If user email is present and no authentication is set in context
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        // Load user details
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

        // Validate token
        if (jwtService.validateToken(jwt, userDetails.getUsername())) {
          // Create authentication token
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              userDetails.getAuthorities()
          );

          // Set authentication details
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          // Set authentication in security context
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      // Invalid token - continue without authentication
      logger.error("JWT validation failed", e);
    }

    filterChain.doFilter(request, response);
  }

  private String extractToken(HttpServletRequest request) {
    final String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }

    return null;
  }
}
