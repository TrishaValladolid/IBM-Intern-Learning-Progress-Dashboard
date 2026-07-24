package com.dashboard.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Application user account. Only two roles exist in this system:
 * ADMIN (Program/Training Coordinator) and TRAINER.
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** PBKDF2 hash in the form "iterations:base64Salt:base64Hash". Never store plaintext. */
    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    /** Whether the account may log in. Disabled accounts are rejected at authentication. */
    @Column(name = "enabled", nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    // Empty means the trainer is not restricted to particular training areas.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_training_assignment", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "training_name", length = 255)
    private Set<String> assignedTrainings = new LinkedHashSet<>();

    public User() {}

    public User(String username, String passwordHash, Role role, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
    }

    @PrePersist
    void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public Set<String> getAssignedTrainings() { return assignedTrainings; }
    public void setAssignedTrainings(Set<String> assignedTrainings) {
        this.assignedTrainings = assignedTrainings == null ? new LinkedHashSet<>() : new LinkedHashSet<>(assignedTrainings);
    }

    public enum Role {
        ADMIN,
        TRAINER
    }
}
