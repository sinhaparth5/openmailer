package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttributesAdvice {

    private final boolean assetMinified;

    public GlobalViewAttributesAdvice(@Value("${app.assets.minified:false}") boolean assetMinified) {
        this.assetMinified = assetMinified;
    }

    @ModelAttribute("assetMinified")
    public boolean assetMinified() {
        return assetMinified;
    }

    @ModelAttribute("currentUserDisplayName")
    public String currentUserDisplayName() {
        User user = currentUser();
        if (user == null) {
            return null;
        }

        String fullName = ((user.getFirstName() != null ? user.getFirstName().trim() : "")
            + " "
            + (user.getLastName() != null ? user.getLastName().trim() : "")).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }

        return user.getEmail();
    }

    @ModelAttribute("currentUserInitials")
    public String currentUserInitials() {
        User user = currentUser();
        if (user == null) {
            return null;
        }

        StringBuilder initials = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            initials.append(Character.toUpperCase(user.getFirstName().trim().charAt(0)));
        }
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            initials.append(Character.toUpperCase(user.getLastName().trim().charAt(0)));
        }
        if (initials.length() == 0 && user.getUsername() != null && !user.getUsername().isBlank()) {
            initials.append(Character.toUpperCase(user.getUsername().trim().charAt(0)));
        }
        if (initials.length() == 0 && user.getEmail() != null && !user.getEmail().isBlank()) {
            initials.append(Character.toUpperCase(user.getEmail().trim().charAt(0)));
        }

        return initials.toString();
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }
        return userDetails.getUser();
    }
}
