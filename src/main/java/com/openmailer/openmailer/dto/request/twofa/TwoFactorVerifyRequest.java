package com.openmailer.openmailer.dto.request.twofa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for verifying 2FA codes.
 */
public class TwoFactorVerifyRequest {

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{6}$|^[A-Z0-9]{8}$", message = "Code must be either a 6-digit TOTP code or 8-character backup code")
    private String code;

    public TwoFactorVerifyRequest() {
    }

    public TwoFactorVerifyRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
