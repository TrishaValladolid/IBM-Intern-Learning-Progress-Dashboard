package com.dashboard.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Teacher feedback left on an intern by a trainer or admin. Mirrors the shape of
 * {@link Training}: the owning intern is a LAZY @ManyToOne kept out of the JSON
 * (@JsonbTransient) so serializing the entity never touches the lazy relation.
 *
 * Ownership is tracked by {@code authorUsername} (the stable login), never by the
 * display name. {@code authorName} is a snapshot of the author's full name captured
 * at write time for display. {@code createdAt} is stamped once on insert; {@code
 * updatedAt} stays null until the first edit so the UI can show "edited" only when
 * it truly was.
 */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    @JsonbTransient
    private Intern intern;

    // Login of the author. Ownership/edit-delete checks compare against this,
    // never the display name, so a full-name change never transfers ownership.
    @Column(name = "author_username", nullable = false)
    private String authorUsername;

    // Display name captured at write time (full name, or username fallback).
    @Column(name = "author_name")
    private String authorName;

    @Column(name = "content", length = 4000, nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Null until the first edit; @PreUpdate stamps it on every subsequent merge.
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Feedback() {}

    public Feedback(Intern intern, String authorUsername, String authorName, String content) {
        this.intern = intern;
        this.authorUsername = authorUsername;
        this.authorName = authorName;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @JsonbTransient
    public Intern getIntern() { return intern; }
    public void setIntern(Intern intern) { this.intern = intern; }

    // Expose the owning intern's id in the JSON without serializing the relation.
    public Long getInternId() { return intern == null ? null : intern.getId(); }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
