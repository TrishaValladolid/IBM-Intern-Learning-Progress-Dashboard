package com.dashboard.dto;

/**
 * Flat grade record for the Assignments and Grades matrix. Returning this
 * instead of the full Submission entity keeps the payload small and avoids
 * serializing the lazy Intern/Assignment relations. Public-field style matches
 * the existing request/response DTOs.
 */
public class GradeCell {
    public Long internId;
    public Long assignmentId;
    public Integer score;

    public GradeCell() {}

    public GradeCell(Long internId, Long assignmentId, Integer score) {
        this.internId = internId;
        this.assignmentId = assignmentId;
        this.score = score;
    }
}
