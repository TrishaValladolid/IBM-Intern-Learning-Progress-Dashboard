package com.dashboard.dto;

import java.util.List;

// Roll-up view of one training batch for the admin Training Batches overview.
// A "batch" is the Intern.batch String (a cohort of interns sharing that value),
// so these figures are computed by aggregating the interns in the batch — no
// Batch entity exists or is needed.
public class BatchSummary {
    private String batch;
    private int internCount;
    private List<String> tracks;
    private int trainingCount;
    private int assignmentCount;
    private double avgCompletionPercentage;
    private double avgScorePercentage;

    public BatchSummary(String batch, int internCount, List<String> tracks,
                        int trainingCount, int assignmentCount,
                        double avgCompletionPercentage, double avgScorePercentage) {
        this.batch = batch;
        this.internCount = internCount;
        this.tracks = tracks;
        this.trainingCount = trainingCount;
        this.assignmentCount = assignmentCount;
        this.avgCompletionPercentage = avgCompletionPercentage;
        this.avgScorePercentage = avgScorePercentage;
    }

    public String getBatch() { return batch; }
    public int getInternCount() { return internCount; }
    public List<String> getTracks() { return tracks; }
    public int getTrainingCount() { return trainingCount; }
    public int getAssignmentCount() { return assignmentCount; }
    public double getAvgCompletionPercentage() { return avgCompletionPercentage; }
    public double getAvgScorePercentage() { return avgScorePercentage; }
}
