package com.dashboard.dto;

/**
 * Dynamically calculated attendance statistics.
 * Attendance percentage = present / total recorded entries (as a percentage).
 * Mirrors the ProgressSummary calculated-summary DTO pattern.
 */
public class AttendanceSummary {
    private long totalPresent;
    private long totalLate;
    private long totalAbsent;
    private double attendancePercentage;

    public AttendanceSummary(long totalPresent, long totalLate, long totalAbsent,
                             double attendancePercentage) {
        this.totalPresent = totalPresent;
        this.totalLate = totalLate;
        this.totalAbsent = totalAbsent;
        this.attendancePercentage = attendancePercentage;
    }

    public long getTotalPresent() { return totalPresent; }
    public long getTotalLate() { return totalLate; }
    public long getTotalAbsent() { return totalAbsent; }
    public double getAttendancePercentage() { return attendancePercentage; }
}
