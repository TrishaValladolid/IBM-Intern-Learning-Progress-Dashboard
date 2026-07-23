package com.dashboard.dto;

import java.util.List;

/**
 * Bulk save for the roster-sheet workflow: the whole class marked in one request.
 * The trainer presses Save once and every intern's status arrives together.
 * Public-field style matches the existing request DTOs.
 */
public class AttendanceBulkRequest {
    public String batch;
    public String date;
    public List<Entry> records;

    public static class Entry {
        public Long internId;
        public String status;
    }
}
