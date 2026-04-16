package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.UserRepository;
import com.openmailer.openmailer.service.security.PasswordEncoderService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderService passwordEncoderService;

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
            userRepository,
            passwordEncoderService,
            mailSender,
            "http://localhost:8080",
            "",
            "noreply@example.com"
        );
    }

    @Test
    void requestPasswordResetStoresTokenHashAndSendsEmail() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("owner@example.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        passwordResetService.requestPasswordReset("owner@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getPasswordResetTokenHash());
        assertEquals(64, savedUser.getPasswordResetTokenHash().length());
        assertNotNull(savedUser.getPasswordResetTokenExpiresAt());
        assertTrue(savedUser.getPasswordResetTokenExpiresAt().isAfter(LocalDateTime.now()));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void resetPasswordUpdatesPasswordAndClearsRecoveryState() {
        User user = new User();
        user.setId("user-1");
        user.setFailedLoginAttempts(4);
        user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(any(), any(LocalDateTime.class)))
            .thenReturn(Optional.of(user));
        when(passwordEncoderService.encode("NewPassword1!")).thenReturn("encoded-password");

        passwordResetService.resetPassword("raw-token", "NewPassword1!");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encoded-password", savedUser.getPassword());
        assertNull(savedUser.getPasswordResetTokenHash());
        assertNull(savedUser.getPasswordResetTokenExpiresAt());
        assertEquals(0, savedUser.getFailedLoginAttempts());
        assertNull(savedUser.getAccountLockedUntil());
    }

    @Test
    void resetPasswordRejectsExpiredOrUnknownToken() {
        when(userRepository.findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(any(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        ValidationException ex = assertThrows(
            ValidationException.class,
            () -> passwordResetService.resetPassword("bad-token", "NewPassword1!")
        );

        assertEquals("token", ex.getField());
    }

    @Test
    void isResetTokenValidReturnsFalseWhenNoUserMatches() {
        when(userRepository.findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(any(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        assertFalse(passwordResetService.isResetTokenValid("missing-token"));
    }
}
