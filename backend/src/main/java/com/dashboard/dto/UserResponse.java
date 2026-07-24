package com.dashboard.dto;

import com.dashboard.entity.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Safe view of a {@link User} for the Trainer Management module.
 * Deliberately omits the password hash so it never leaves the backend.
 */
public class UserResponse {
    public Long id;
    public String username;
    public String fullName;
    public String email;
    public String role;
    public boolean enabled;
    public String createdDate;
    public List<String> assignedTrainings;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.fullName = user.getFullName();
        dto.email = user.getEmail();
        dto.role = user.getRole() != null ? user.getRole().name() : null;
        dto.enabled = user.isEnabled();
        dto.createdDate = user.getCreatedDate() != null ? user.getCreatedDate().toString() : null;
        dto.assignedTrainings = new ArrayList<>(user.getAssignedTrainings());
        return dto;
    }
}
