package com.dashboard.dto;

/** Payload for creating a trainer account. First/last name are combined into full name. */
public class CreateTrainerRequest {
    public String firstName;
    public String lastName;
    public String username;
    public String email;
    public String password;
    public String confirmPassword;
    public String role;
}
