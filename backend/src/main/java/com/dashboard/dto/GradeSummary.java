package com.dashboard.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-intern grade roll-up, centralised on the backend so the frontend no longer
 * has to replicate the grouping/total math.
 *
 * Scores come from the intern's recorded submissions joined to their assignments.
 * Assignments are grouped by their training/category; an assignment with no
 * training (blank trainingName) is surfaced on its own in {@code independentAssignments}
 * rather than lumped into a catch-all group.
 *
 * Missing scores are never treated as zero: a submission that is pending/submitted
 * but ungraded carries a null {@code score} and contributes to neither
 * {@code totalScore} nor {@code totalMaxScore}. A category total is therefore the
 * sum over its graded assignments only, matching the previous client-side rule.
 */
public class GradeSummary {

    public Long internId;
    public List<Category> categories = new ArrayList<>();
    public List<AssignmentScore> independentAssignments = new ArrayList<>();

    public GradeSummary() {}

    public GradeSummary(Long internId) {
        this.internId = internId;
    }

    // One training/category with its contributing assignments and a combined total.
    public static class Category {
        public String trainingName;
        // Sum of scores over graded assignments only; null when none are graded.
        public Integer totalScore;
        // Sum of maxScore over those same graded assignments.
        public Integer totalMaxScore;
        // Rounded percentage of totalScore/totalMaxScore; null when nothing graded.
        public Integer totalPercentage;
        public int gradedCount;
        public int assignmentCount;
        public List<AssignmentScore> assignments = new ArrayList<>();

        public Category() {}

        public Category(String trainingName) {
            this.trainingName = trainingName;
        }
    }

    // A single assignment's score line. score/status are null-safe: an ungraded
    // submission keeps score null and reports its real status (PENDING/SUBMITTED).
    public static class AssignmentScore {
        public Long assignmentId;
        public String title;
        public Integer score;      // null when not graded — never coerced to 0
        public Integer maxScore;
        public Integer percentage; // null unless score and a positive maxScore exist
        public String status;      // PENDING | SUBMITTED | GRADED

        public AssignmentScore() {}

        public AssignmentScore(Long assignmentId, String title, Integer score,
                               Integer maxScore, Integer percentage, String status) {
            this.assignmentId = assignmentId;
            this.title = title;
            this.score = score;
            this.maxScore = maxScore;
            this.percentage = percentage;
            this.status = status;
        }
    }
}
