package com.dashboard.dto;

/** Payload for the admin "Reset Password" action. */
public class PasswordResetRequest {
    public String newPassword;
    public String confirmPassword;
}
