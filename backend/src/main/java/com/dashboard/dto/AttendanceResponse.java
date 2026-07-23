package com.dashboard.dto;

import com.dashboard.entity.Attendance;

/**
 * Flattened view of an {@link Attendance} record for the table:
 * Date, Intern ID (talentId), Intern Name, Status, Recorded By.
 * Mirrors the UserResponse.from(...) factory pattern.
 */
public class AttendanceResponse {
    public Long id;
    public Long internId;
    public String talentId;
    public String internName;
    public String date;
    public String status;
    public String recordedBy;

    public static AttendanceResponse from(Attendance a) {
        AttendanceResponse dto = new AttendanceResponse();
        dto.id = a.getId();
        if (a.getIntern() != null) {
            dto.internId = a.getIntern().getId();
            dto.talentId = a.getIntern().getTalentId();
            dto.internName = a.getIntern().getName();
        }
        dto.date = a.getDate() != null ? a.getDate().toString() : null;
        dto.status = a.getStatus() != null ? a.getStatus().name() : null;
        dto.recordedBy = a.getRecordedBy();
        return dto;
    }
}
