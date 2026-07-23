package com.dashboard.dto;

// Rename a training batch: move every intern (and its assignments) from the
// old batch string to a new one. Public fields match the existing request-DTO
// style used elsewhere (e.g. SubmissionRequest, AttendanceBulkRequest).
public class RenameBatchRequest {
    public String oldBatch;
    public String newBatch;
}
