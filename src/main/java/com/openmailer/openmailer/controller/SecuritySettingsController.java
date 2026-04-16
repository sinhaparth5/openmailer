package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.auth.TwoFactorAuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecuritySettingsController {

  private final TwoFactorAuthService twoFactorAuthService;

  public SecuritySettingsController(TwoFactorAuthService twoFactorAuthService) {
    this.twoFactorAuthService = twoFactorAuthService;
  }

  @GetMapping("/settings/security")
  public String securitySettings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    String userId = userDetails.getUser().getId();

    model.addAttribute("pageTitle", "Security Settings - OpenMailer");
    model.addAttribute("currentUser", userDetails.getUser());
    model.addAttribute("twoFactorEnabled", twoFactorAuthService.isTwoFactorEnabled(userId));
    model.addAttribute("backupCodeCount", twoFactorAuthService.getRemainingBackupCodes(userId).size());
    model.addAttribute("lastLoginAt", userDetails.getUser().getLastLoginAt());

    return "settings/security";
  }
}
