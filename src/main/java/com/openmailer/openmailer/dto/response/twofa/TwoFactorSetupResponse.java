package com.openmailer.openmailer.dto.response.twofa;

/**
 * DTO for 2FA setup response containing secret and QR code.
 */
public class TwoFactorSetupResponse {

    private String secret;
    private String qrCodeDataUrl;
    private String message;

    public TwoFactorSetupResponse() {
    }

    public TwoFactorSetupResponse(String secret, String qrCodeDataUrl, String message) {
        this.secret = secret;
        this.qrCodeDataUrl = qrCodeDataUrl;
        this.message = message;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getQrCodeDataUrl() {
        return qrCodeDataUrl;
    }

    public void setQrCodeDataUrl(String qrCodeDataUrl) {
        this.qrCodeDataUrl = qrCodeDataUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
