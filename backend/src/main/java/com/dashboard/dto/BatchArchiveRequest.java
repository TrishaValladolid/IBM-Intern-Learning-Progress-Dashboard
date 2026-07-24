package com.dashboard.dto;

// Archive every intern in a batch to one non-active status (ADMIN). The status
// is one of the Intern.Status names but must not be ACTIVE; validation happens
// in the resource. Public fields match the existing request-DTO style.
public class BatchArchiveRequest {
    public String batch;
    public String status;
}
