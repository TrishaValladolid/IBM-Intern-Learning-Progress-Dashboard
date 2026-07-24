package com.dashboard.dto;

/** Payload for editing a trainer's profile. Password is never updated here. */
public class UpdateTrainerRequest {
    public String fullName;
    public String username;
    public String email;
    public java.util.List<String> assignedTrainings;
}
