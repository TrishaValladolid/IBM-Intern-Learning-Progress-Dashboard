package com.dashboard.dto;

/**
 * One row of the attendance roster sheet: an intern in the selected training,
 * prefilled with any status already recorded for the selected date (null when
 * attendance has not yet been taken). Mirrors the AttendanceResponse public-field
 * DTO style.
 */
public class AttendanceRosterEntry {
    public Long internId;
    public String name;
    public String talentId;
    public String track;
    public String status;

    public AttendanceRosterEntry(Long internId, String name, String talentId,
                                 String track, String status) {
        this.internId = internId;
        this.name = name;
        this.talentId = talentId;
        this.track = track;
        this.status = status;
    }
}
