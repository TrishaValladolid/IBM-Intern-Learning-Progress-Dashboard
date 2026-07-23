package com.dashboard.dto;

/**
 * Payload for assigning one training to every intern in a batch at once.
 * Public-field style matches the existing request DTOs. repoUrl holds an
 * optional Box Drive link (the shared folder where interns upload their work);
 * null/blank means "no link". batch is the cohort to assign the training to.
 */
public class BatchTrainingRequest {
    public String trainingName;
    public String repoUrl;
    public String batch;
}
