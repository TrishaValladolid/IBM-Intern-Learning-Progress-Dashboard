package com.dashboard.dto;

/** Returned on successful login. The token is a signed bearer token. */
public class LoginResponse {
    public String token;
    public String username;
    public String role;
    public String fullName;

    public LoginResponse(String token, String username, String role, String fullName) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }
}
