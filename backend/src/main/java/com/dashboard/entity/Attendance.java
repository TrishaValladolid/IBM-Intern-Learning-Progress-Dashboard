package com.dashboard.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    private Intern intern;

    // "date" is a reserved word in several databases, so map to attendance_date.
    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    // Username of the trainer who recorded the entry (from the security context).
    @Column(name = "recorded_by")
    private String recordedBy;

    // The roster session this record belongs to (training + date grouping).
    // Nullable so pre-existing records taken before sessions were introduced
    // still load; the (batch, date) uniqueness lives on AttendanceSession.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AttendanceSession session;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public enum Status { PRESENT, LATE, ABSENT }

    public Attendance() {}

    public Attendance(Intern intern, LocalDate date, Status status, String recordedBy) {
        this.intern = intern;
        this.date = date;
        this.status = status;
        this.recordedBy = recordedBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Intern getIntern() { return intern; }
    public void setIntern(Intern intern) { this.intern = intern; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public AttendanceSession getSession() { return session; }
    public void setSession(AttendanceSession session) { this.session = session; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
