package com.dashboard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "intern")
public class Intern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(name = "talent_id")
    private String talentId;

    private String batch;

    private String track;

    // Lifecycle status. New interns are ACTIVE; archival moves them to one of the
    // non-active values. Stored as its name (EnumType.STRING) like Submission.Status
    // and User.Role. Legacy rows created before this column existed have NULL here
    // and are treated as ACTIVE — see getStatus().
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Status status = Status.ACTIVE;

    // ---- Profile detail fields ----
    // All nullable so hbm2ddl.auto=update can add the columns to existing rows
    // without a migration; legacy interns simply load with NULL here.

    // Total hours the intern is expected to complete. Zero or a positive number.
    @Column(name = "total_hours_required")
    private Double totalHoursRequired;

    // ISO yyyy-MM-dd strings, matching how Assignment.dueDate stores dates so they
    // round-trip cleanly with the <input type="date"> controls on the frontend.
    @Column(name = "expected_graduation_date")
    private String expectedGraduationDate;

    @Column(name = "expected_internship_end_date")
    private String expectedInternshipEndDate;

    private String school;

    private String course;

    public enum Status {
        ACTIVE,
        OFFBOARDED,
        CONVERTED_TO_EMPLOYEE,
        WITHDRAWN,
        TERMINATED
    }

    public Intern() {}

    public Intern(String name, String talentId, String batch, String track) {
        this.name = name;
        this.talentId = talentId;
        this.batch = batch;
        this.track = track;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTalentId() { return talentId; }
    public void setTalentId(String talentId) { this.talentId = talentId; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }

    // Coalesce legacy NULLs to ACTIVE so pre-existing interns behave and
    // serialize as active without a data migration.
    public Status getStatus() { return status == null ? Status.ACTIVE : status; }
    public void setStatus(Status status) { this.status = status; }

    public Double getTotalHoursRequired() { return totalHoursRequired; }
    public void setTotalHoursRequired(Double totalHoursRequired) { this.totalHoursRequired = totalHoursRequired; }

    public String getExpectedGraduationDate() { return expectedGraduationDate; }
    public void setExpectedGraduationDate(String expectedGraduationDate) { this.expectedGraduationDate = expectedGraduationDate; }

    public String getExpectedInternshipEndDate() { return expectedInternshipEndDate; }
    public void setExpectedInternshipEndDate(String expectedInternshipEndDate) { this.expectedInternshipEndDate = expectedInternshipEndDate; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

}
