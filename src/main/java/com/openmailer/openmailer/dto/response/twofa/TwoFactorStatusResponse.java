package com.openmailer.openmailer.dto.response.twofa;

public class TwoFactorStatusResponse {

  private boolean enabled;
  private int backupCodesRemaining;

  public TwoFactorStatusResponse() {
  }

  public TwoFactorStatusResponse(boolean enabled, int backupCodesRemaining) {
    this.enabled = enabled;
    this.backupCodesRemaining = backupCodesRemaining;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getBackupCodesRemaining() {
    return backupCodesRemaining;
  }

  public void setBackupCodesRemaining(int backupCodesRemaining) {
    this.backupCodesRemaining = backupCodesRemaining;
  }
}
