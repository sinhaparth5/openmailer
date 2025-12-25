package com.openmailer.openmailer.dto.response.twofa;

import java.util.List;

/**
 * DTO for 2FA backup codes response.
 */
public class TwoFactorBackupCodesResponse {

    private List<String> backupCodes;
    private String message;

    public TwoFactorBackupCodesResponse() {
    }

    public TwoFactorBackupCodesResponse(List<String> backupCodes, String message) {
        this.backupCodes = backupCodes;
        this.message = message;
    }

    public List<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
