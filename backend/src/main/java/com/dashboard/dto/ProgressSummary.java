package com.dashboard.dto;

public class ProgressSummary {
    private Long internId;
    private String internName;
    private int totalAssignments;
    private int completedAssignments;
    private double completionPercentage;
    private double averageScorePercentage;

    public ProgressSummary(Long internId, String internName, int totalAssignments,
                            int completedAssignments, double completionPercentage,
                            double averageScorePercentage) {
        this.internId = internId;
        this.internName = internName;
        this.totalAssignments = totalAssignments;
        this.completedAssignments = completedAssignments;
        this.completionPercentage = completionPercentage;
        this.averageScorePercentage = averageScorePercentage;
    }

    public Long getInternId() { return internId; }
    public String getInternName() { return internName; }
    public int getTotalAssignments() { return totalAssignments; }
    public int getCompletedAssignments() { return completedAssignments; }
    public double getCompletionPercentage() { return completionPercentage; }
    public double getAverageScorePercentage() { return averageScorePercentage; }
}
