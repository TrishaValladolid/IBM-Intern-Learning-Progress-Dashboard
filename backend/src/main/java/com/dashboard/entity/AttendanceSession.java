package com.dashboard.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One attendance session groups the attendance records taken for a single
 * training (batch) on a single date. The unique (batch, session_date) constraint
 * enforces "one session per training per date" — the backbone of duplicate
 * prevention for the roster-sheet workflow. Individual {@link Attendance} records
 * link back to their session via a nullable FK.
 */
@Entity
@Table(name = "attendance_session",
        uniqueConstraints = @UniqueConstraint(columnNames = {"batch", "session_date"}))
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "Training" maps to the intern batch (no separate Training entity exists).
    @Column(nullable = false)
    private String batch;

    // "date" is a reserved word in several databases, so map to session_date.
    @Column(name = "session_date", nullable = false)
    private LocalDate date;

    // Display name (or username) of the trainer who recorded the session.
    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public AttendanceSession() {}

    public AttendanceSession(String batch, LocalDate date, String recordedBy) {
        this.batch = batch;
        this.date = date;
        this.recordedBy = recordedBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
