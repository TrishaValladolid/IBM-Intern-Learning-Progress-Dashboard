package com.dashboard.resource;

import com.dashboard.dto.AttendanceBulkRequest;
import com.dashboard.dto.AttendanceResponse;
import com.dashboard.dto.AttendanceRosterEntry;
import com.dashboard.dto.AttendanceSummary;
import com.dashboard.entity.Attendance;
import com.dashboard.entity.AttendanceSession;
import com.dashboard.entity.Intern;
import com.dashboard.entity.User;
import com.dashboard.repository.AttendanceRepository;
import com.dashboard.repository.AttendanceSessionRepository;
import com.dashboard.repository.InternRepository;
import com.dashboard.repository.UserRepository;
import com.dashboard.security.Secured;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Attendance Management.
 *
 * Security mirrors InternResource: the class-level @Secured installs the security
 * context so only authenticated users reach any method. GET methods carry no role
 * annotation, so both ADMIN and TRAINER may view/filter/summarize. Mutating methods
 * (record, edit) require the TRAINER role, matching the existing role restrictions.
 */
@Path("/attendance")
@Secured
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AttendanceResource {

    @Inject
    private AttendanceRepository attendanceRepository;
    @Inject
    private AttendanceSessionRepository sessionRepository;
    @Inject
    private InternRepository internRepository;
    @Inject
    private UserRepository userRepository;

    @Context
    private SecurityContext securityContext;

    // Admins see every intern; trainers only ACTIVE ones (legacy NULL counts as
    // active). Enforced here so a trainer cannot reach an archived record through
    // an attendance endpoint by calling the API directly.
    private boolean isAdmin() {
        return securityContext.isUserInRole("ADMIN");
    }

    // True when the caller may see this intern: admins always, trainers only
    // while the intern is ACTIVE.
    private boolean canView(Intern intern) {
        return isAdmin() || intern.getStatus() == Intern.Status.ACTIVE;
    }

    // ---- View (ADMIN + TRAINER) ----

    @GET
    public List<AttendanceResponse> getAll() {
        return attendanceRepository.findAll().stream()
                // Trainers must not receive attendance for archived interns.
                .filter(a -> canView(a.getIntern()))
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/interns/{internId}/history")
    public Response getHistory(@PathParam("internId") Long internId) {
        Intern intern = internRepository.findById(internId);
        // Trainers get 404 for archived interns so the endpoint does not reveal
        // that an archived record exists.
        if (intern == null || !canView(intern)) {
            return error(Response.Status.NOT_FOUND, "Intern not found.");
        }
        List<AttendanceResponse> history = attendanceRepository.findByInternId(internId).stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
        return Response.ok(history).build();
    }

    // Overall summary, or per-intern when ?internId= is supplied. Counts are computed
    // dynamically in the database.
    @GET
    @Path("/summary")
    public Response getSummary(@QueryParam("internId") Long internId) {
        long present;
        long late;
        long absent;
        if (internId != null) {
            Intern intern = internRepository.findById(internId);
            if (intern == null || !canView(intern)) {
                return error(Response.Status.NOT_FOUND, "Intern not found.");
            }
            present = attendanceRepository.countByInternIdAndStatus(internId, Attendance.Status.PRESENT);
            late = attendanceRepository.countByInternIdAndStatus(internId, Attendance.Status.LATE);
            absent = attendanceRepository.countByInternIdAndStatus(internId, Attendance.Status.ABSENT);
        } else {
            present = attendanceRepository.countByStatus(Attendance.Status.PRESENT);
            late = attendanceRepository.countByStatus(Attendance.Status.LATE);
            absent = attendanceRepository.countByStatus(Attendance.Status.ABSENT);
        }
        long total = present + late + absent;
        double percentage = total == 0 ? 0.0 : round2(present * 100.0 / total);
        return Response.ok(new AttendanceSummary(present, late, absent, percentage)).build();
    }

    // ---- Roster sheet (ADMIN + TRAINER) ----

    // Distinct trainings (batches) for the "Select Training" dropdown.
    @GET
    @Path("/trainings")
    public List<String> getTrainings() {
        // Trainers only see batches that still have active interns.
        return isAdmin() ? internRepository.findDistinctBatches()
                : internRepository.findDistinctActiveBatches();
    }

    // Every intern in the selected training, prefilled with any status already
    // recorded on that date. This is the classroom roster the trainer marks.
    @GET
    @Path("/roster")
    public Response getRoster(@QueryParam("batch") String batch, @QueryParam("date") String date) {
        if (batch == null || batch.isBlank()) {
            return error(Response.Status.BAD_REQUEST, "A training is required.");
        }
        LocalDate day = parseDate(date);
        if (day == null) {
            return error(Response.Status.BAD_REQUEST, "A valid date is required.");
        }
        return Response.ok(buildRoster(batch, day)).build();
    }

    // ---- Bulk record / edit (TRAINER) ----

    // Single write path for the whole class. Find-or-create the (training, date)
    // session, then upsert each intern's record so re-saving edits in place rather
    // than duplicating. Returns the refreshed roster.
    @POST
    @Path("/bulk")
    @RolesAllowed("TRAINER")
    public Response saveBulk(AttendanceBulkRequest req, @Context SecurityContext securityContext) {
        if (req == null) {
            return error(Response.Status.BAD_REQUEST, "Request body is required.");
        }
        if (req.batch == null || req.batch.isBlank()) {
            return error(Response.Status.BAD_REQUEST, "A training is required.");
        }
        LocalDate day = parseDate(req.date);
        if (day == null) {
            return error(Response.Status.BAD_REQUEST, "A valid date is required.");
        }
        if (req.records == null || req.records.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "At least one attendance record is required.");
        }

        // Find-or-create the session that groups this training/date's records.
        AttendanceSession session = sessionRepository.findByBatchAndDate(req.batch, day);
        if (session == null) {
            session = new AttendanceSession(req.batch, day, recorderName(securityContext));
        } else {
            session.setRecordedBy(recorderName(securityContext));
        }
        session = sessionRepository.save(session);

        // Index existing records for this training/date so we update in place.
        Map<Long, Attendance> existingByIntern = new HashMap<>();
        for (Attendance a : attendanceRepository.findByBatchAndDate(req.batch, day)) {
            existingByIntern.put(a.getIntern().getId(), a);
        }

        String recorder = recorderName(securityContext);
        for (AttendanceBulkRequest.Entry entry : req.records) {
            if (entry == null || entry.internId == null) {
                continue;
            }
            Attendance.Status status = parseStatus(entry.status);
            if (status == null) {
                return error(Response.Status.BAD_REQUEST,
                        "A valid status is required for every intern.");
            }
            Intern intern = internRepository.findById(entry.internId);
            if (intern == null) {
                return error(Response.Status.NOT_FOUND,
                        "Intern not found: " + entry.internId + ".");
            }
            // Only accept interns that belong to the selected training, and skip
            // any the caller may not see (archived interns for a trainer) so a
            // direct API call cannot mark attendance on an archived record.
            if (!req.batch.equals(intern.getBatch()) || !canView(intern)) {
                continue;
            }

            Attendance record = existingByIntern.get(intern.getId());
            if (record == null) {
                record = new Attendance(intern, day, status, recorder);
            } else {
                record.setStatus(status);
            }
            record.setSession(session);
            attendanceRepository.save(record);
        }

        return Response.ok(buildRoster(req.batch, day)).build();
    }

    // ---- Helpers ----

    // Builds the roster rows for a training/date: every intern in the batch, each
    // carrying its recorded status for that date (or null when not yet marked).
    private List<AttendanceRosterEntry> buildRoster(String batch, LocalDate day) {
        Map<Long, Attendance.Status> statusByIntern = new HashMap<>();
        for (Attendance a : attendanceRepository.findByBatchAndDate(batch, day)) {
            statusByIntern.put(a.getIntern().getId(), a.getStatus());
        }
        // Admins see the whole cohort; trainers only its active interns.
        List<Intern> roster = isAdmin() ? internRepository.findByBatch(batch)
                : internRepository.findByBatchActive(batch);
        return roster.stream()
                .map(i -> {
                    Attendance.Status status = statusByIntern.get(i.getId());
                    return new AttendanceRosterEntry(
                            i.getId(),
                            i.getName(),
                            i.getTalentId(),
                            i.getTrack(),
                            status == null ? null : status.name());
                })
                .collect(Collectors.toList());
    }

    // Prefer the trainer's full name for display; fall back to the username.
    private String recorderName(SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return null;
        }
        String username = securityContext.getUserPrincipal().getName();
        User user = userRepository.findByUsername(username);
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return username;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Attendance.Status parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Attendance.Status.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity("{\"error\":\"" + message + "\"}")
                .build();
    }
}
