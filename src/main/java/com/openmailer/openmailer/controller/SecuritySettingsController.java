package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.auth.TwoFactorAuthService;
import com.openmailer.openmailer.service.auth.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.TimeUnit;

@Controller
public class SecuritySettingsController {

  private final TwoFactorAuthService twoFactorAuthService;
  private final UserService userService;

  public SecuritySettingsController(TwoFactorAuthService twoFactorAuthService, UserService userService) {
    this.twoFactorAuthService = twoFactorAuthService;
    this.userService = userService;
  }

  @GetMapping("/settings/security")
  public String securitySettings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    String userId = userDetails.getUser().getId();

    model.addAttribute("pageTitle", "Security Settings - OpenMailer");
    model.addAttribute("currentUser", userDetails.getUser());
    model.addAttribute("twoFactorEnabled", twoFactorAuthService.isTwoFactorEnabled(userId));
    model.addAttribute("backupCodeCount", twoFactorAuthService.getRemainingBackupCodes(userId).size());
    model.addAttribute("lastLoginAt", userDetails.getUser().getLastLoginAt());
    model.addAttribute("profileImageMaxBytes", 2 * 1024 * 1024);

    return "settings/security";
  }

  @GetMapping("/users/me/avatar")
  @ResponseBody
  public ResponseEntity<byte[]> currentUserAvatar(@AuthenticationPrincipal CustomUserDetails userDetails) {
    var profileImage = userService.loadProfileImage(userDetails.getUser().getId());
    if (profileImage == null || profileImage.data() == null || profileImage.data().length == 0) {
      return ResponseEntity.notFound().build();
    }

    MediaType mediaType;
    try {
      mediaType = profileImage.contentType() != null
          ? MediaType.parseMediaType(profileImage.contentType())
          : MediaType.APPLICATION_OCTET_STREAM;
    } catch (IllegalArgumentException ex) {
      mediaType = MediaType.APPLICATION_OCTET_STREAM;
    }

    return ResponseEntity.ok()
        .contentType(mediaType)
        .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(profileImage.data().length))
        .body(profileImage.data());
  }
}
