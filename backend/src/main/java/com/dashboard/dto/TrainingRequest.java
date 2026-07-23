package com.dashboard.dto;

/**
 * Edit payload for an intern's training. Public-field style matches the
 * existing request DTOs. Only the training name is editable per intern; the
 * Box Drive link is owned by the training at batch-assign time.
 */
public class TrainingRequest {
    public String trainingName;
}
