package com.dashboard.dto;

// Change a single intern's lifecycle status (ADMIN). The value is one of the
// Intern.Status names; validation happens in the resource. Public field matches
// the existing request-DTO style (e.g. RenameBatchRequest).
public class InternStatusRequest {
    public String status;
}
