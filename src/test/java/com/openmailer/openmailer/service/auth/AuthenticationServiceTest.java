package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.dto.request.auth.LoginRequest;
import com.openmailer.openmailer.dto.response.auth.LoginResponse;
import com.openmailer.openmailer.exception.UnauthorizedException;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.service.security.JwtService;
import com.openmailer.openmailer.service.security.PasswordEncoderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoderService passwordEncoderService;

    @Mock
    private JwtService jwtService;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            userService,
            passwordEncoderService,
            jwtService,
            twoFactorAuthService
        );
    }

    @Test
    void loginRecordsSuccessfulLoginTimestamp() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("owner@example.com");
        user.setUsername("owner");
        user.setPassword("encoded-password");
        user.setRole("USER");
        user.setEnabled(true);
        user.setTwoFactorEnabled(false);
        user.setFailedLoginAttempts(2);
        user.setAccountLockedUntil(LocalDateTime.now().minusMinutes(10));

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@example.com");
        request.setPassword("plain-password");

        when(userService.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoderService.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("user-1", "owner@example.com", "owner", "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("user-1", "owner@example.com", "owner", "USER")).thenReturn("refresh-token");
        when(userService.recordSuccessfulLogin(eq("user-1"), any(LocalDateTime.class))).thenReturn(user);

        LoginResponse response = authenticationService.login(request);

        ArgumentCaptor<LocalDateTime> loginTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userService).recordSuccessfulLogin(eq("user-1"), loginTimeCaptor.capture());
        assertNotNull(loginTimeCaptor.getValue());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void loginWithUnknownEmailReturnsGenericUnauthorizedError() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("plain-password");

        when(userService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void loginWithInvalidPasswordRecordsFailedAttempt() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("owner@example.com");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(1);
        user.setEnabled(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@example.com");
        request.setPassword("wrong-password");

        when(userService.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoderService.matches("wrong-password", "encoded-password")).thenReturn(false);

        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userService).recordFailedLoginAttempt("user-1", null);
    }

    @Test
    void loginLocksAccountAfterFifthFailedAttempt() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("owner@example.com");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(4);
        user.setEnabled(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@example.com");
        request.setPassword("wrong-password");

        when(userService.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoderService.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(request));

        ArgumentCaptor<LocalDateTime> lockUntilCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userService).recordFailedLoginAttempt(eq("user-1"), lockUntilCaptor.capture());
        assertNotNull(lockUntilCaptor.getValue());
    }

    @Test
    void loginRejectsLockedAccountBeforePasswordCheck() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("owner@example.com");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(5));

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@example.com");
        request.setPassword("plain-password");

        when(userService.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Account is locked. Try again later", ex.getMessage());
        verify(userService, never()).recordFailedLoginAttempt(eq("user-1"), any(LocalDateTime.class));
        verify(userService, never()).recordSuccessfulLogin(eq("user-1"), any(LocalDateTime.class));
    }
}
