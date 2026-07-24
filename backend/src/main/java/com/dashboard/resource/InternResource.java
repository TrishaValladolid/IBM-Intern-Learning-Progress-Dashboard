package com.dashboard.resource;

import com.dashboard.dto.BatchArchiveRequest;
import com.dashboard.dto.BatchSummary;
import com.dashboard.dto.BatchTrainingRequest;
import com.dashboard.dto.GradeSummary;
import com.dashboard.dto.InternStatusRequest;
import com.dashboard.dto.ProgressSummary;
import com.dashboard.dto.RenameBatchRequest;
import com.dashboard.dto.TrainingRequest;
import com.dashboard.entity.Assignment;
import com.dashboard.entity.Intern;
import com.dashboard.entity.Submission;
import com.dashboard.entity.Training;
import com.dashboard.repository.AssignmentRepository;
import com.dashboard.repository.InternRepository;
import com.dashboard.repository.SubmissionRepository;
import com.dashboard.repository.TrainingRepository;
import com.dashboard.security.Secured;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/interns")
@Secured
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternResource {

    @Inject
    private InternRepository internRepository;
    @Inject
    private AssignmentRepository assignmentRepository;
    @Inject
    private SubmissionRepository submissionRepository;
    @Inject
    private TrainingRepository trainingRepository;
    @Context
    private SecurityContext securityContext;

    // Admins see every intern; trainers only ACTIVE ones (legacy NULL counts as
    // active). Enforced here, not just in the UI, so a trainer cannot reach an
    // archived record by calling the API directly.
    private boolean isAdmin() {
        return securityContext.isUserInRole("ADMIN");
    }

    // True when the caller is allowed to see this intern: admins always, trainers
    // only while the intern is ACTIVE. Used to hide archived records from trainers.
    private boolean canView(Intern intern) {
        return isAdmin() || intern.getStatus() == Intern.Status.ACTIVE;
    }

    @GET
    public List<Intern> getAll() {
        return isAdmin() ? internRepository.findAll() : internRepository.findAllActive();
    }

    @GET
    @Path("/{id}")
    public Response getOne(@PathParam("id") Long id) {
        Intern intern = internRepository.findById(id);
        // Trainers get 404 (not 403) for archived interns so the endpoint does
        // not reveal that an archived record exists.
        if (intern == null || !canView(intern)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(intern).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response create(Intern intern) {
        // Normalise the profile detail fields the same way an edit does.
        intern.setTotalHoursRequired(sanitizeHours(intern.getTotalHoursRequired()));
        intern.setExpectedGraduationDate(blankToNull(intern.getExpectedGraduationDate()));
        intern.setExpectedInternshipEndDate(blankToNull(intern.getExpectedInternshipEndDate()));
        intern.setSchool(blankToNull(intern.getSchool()));
        intern.setCourse(blankToNull(intern.getCourse()));
        Intern saved = internRepository.save(intern);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") Long id, Intern updated) {
        Intern existing = internRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setName(updated.getName());
        existing.setTalentId(updated.getTalentId());
        existing.setBatch(updated.getBatch());
        existing.setTrack(updated.getTrack());
        // Profile detail fields are set directly, including null when a form clears them.
        existing.setTotalHoursRequired(sanitizeHours(updated.getTotalHoursRequired()));
        existing.setExpectedGraduationDate(blankToNull(updated.getExpectedGraduationDate()));
        existing.setExpectedInternshipEndDate(blankToNull(updated.getExpectedInternshipEndDate()));
        existing.setSchool(blankToNull(updated.getSchool()));
        existing.setCourse(blankToNull(updated.getCourse()));
        // Apply status when the edit form supplies one; leave it untouched
        // otherwise so a status-less update does not silently reactivate.
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        return Response.ok(internRepository.save(existing)).build();
    }

    // Change one intern's lifecycle status (ADMIN). Unlike batch archive this may
    // set ACTIVE, so an admin can reactivate an intern that was archived by mistake.
    @PUT
    @Path("/{id}/status")
    @RolesAllowed("ADMIN")
    public Response updateStatus(@PathParam("id") Long id, InternStatusRequest req) {
        if (req == null || req.status == null || req.status.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Status is required.\"}").build();
        }
        Intern.Status status = parseStatus(req.status);
        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid status \\\"" + req.status + "\\\".\"}").build();
        }
        Intern existing = internRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setStatus(status);
        return Response.ok(internRepository.save(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Long id) {
        internRepository.deleteById(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/submissions")
    public Response getSubmissionsForIntern(@PathParam("id") Long internId) {
        Intern intern = internRepository.findById(internId);
        // Trainers must not read an archived intern's submissions via a direct
        // URL; 404 hides the record's existence.
        if (intern == null || !canView(intern)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(submissionRepository.findByInternId(internId)).build();
    }

    // Main feature: overall learning progress for one intern
    @GET
    @Path("/{id}/progress")
    public Response getProgress(@PathParam("id") Long internId) {
        Intern intern = internRepository.findById(internId);
        if (intern == null || !canView(intern)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Intern not found").build();
        }

        int total = assignmentRepository.findAll().size();
        Progress p = computeProgress(internId, total);

        ProgressSummary summary = new ProgressSummary(intern.getId(), intern.getName(), total,
                p.completed, round2(p.completionPct), round2(p.avgScorePct));
        return Response.ok(summary).build();
    }

    // Grade roll-up for one intern, grouped by training/category, computed on the
    // backend so the frontend does not replicate the total math. Ungraded
    // submissions never count as zero — they contribute to neither total. An
    // assignment with no training is returned in independentAssignments.
    @GET
    @Path("/{id}/grades")
    public Response getGrades(@PathParam("id") Long internId) {
        Intern intern = internRepository.findById(internId);
        if (intern == null || !canView(intern)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Intern not found").build();
        }
        return Response.ok(buildGradeSummary(internId)).build();
    }

    // Assemble the GradeSummary from the intern's submissions. Each graded
    // submission maps to one assignment; assignments are bucketed by their
    // trainingName. A blank/absent training name lands in independentAssignments.
    private GradeSummary buildGradeSummary(Long internId) {
        GradeSummary summary = new GradeSummary(internId);

        // Preserve first-seen ordering of categories for a stable UI.
        Map<String, GradeSummary.Category> categories = new LinkedHashMap<>();

        for (Submission s : submissionRepository.findByInternId(internId)) {
            Assignment a = s.getAssignment();
            if (a == null) {
                continue;
            }
            Integer score = s.getScore();
            Integer maxScore = a.getMaxScore();
            // Percentage only when a real score and a positive max exist; never
            // fabricated for an ungraded submission.
            Integer percentage = null;
            if (score != null && maxScore != null && maxScore > 0) {
                percentage = (int) Math.round(score * 100.0 / maxScore);
            }
            String status = s.getStatus() == null ? null : s.getStatus().name();

            GradeSummary.AssignmentScore line = new GradeSummary.AssignmentScore(
                    a.getId(), a.getTitle(), score, maxScore, percentage, status);

            String training = a.getTrainingName() == null ? "" : a.getTrainingName().trim();
            if (training.isEmpty()) {
                // Independent assignment: shown on its own, not lumped into a group.
                summary.independentAssignments.add(line);
                continue;
            }

            GradeSummary.Category category = categories.computeIfAbsent(
                    training, GradeSummary.Category::new);
            category.assignments.add(line);
            category.assignmentCount++;
            // Only graded scores feed the category total; ungraded ones are skipped
            // entirely rather than treated as zero.
            if (score != null && maxScore != null) {
                category.gradedCount++;
                category.totalScore = (category.totalScore == null ? 0 : category.totalScore) + score;
                category.totalMaxScore = (category.totalMaxScore == null ? 0 : category.totalMaxScore) + maxScore;
            }
        }

        // Finalise each category's percentage once its graded totals are known.
        for (GradeSummary.Category category : categories.values()) {
            if (category.totalScore != null && category.totalMaxScore != null
                    && category.totalMaxScore > 0) {
                category.totalPercentage = (int) Math.round(
                        category.totalScore * 100.0 / category.totalMaxScore);
            }
            summary.categories.add(category);
        }
        return summary;
    }

    // Shared progress math for one intern: how many assignments are done and the
    // average score, expressed as percentages. Used by both the per-intern
    // /progress endpoint and the batch roll-up so there is one source of truth.
    private Progress computeProgress(Long internId, int totalAssignments) {
        List<Submission> submissions = submissionRepository.findByInternId(internId);

        long completed = submissions.stream()
                .filter(s -> s.getStatus() == Submission.Status.GRADED
                        || s.getStatus() == Submission.Status.SUBMITTED)
                .count();

        double completionPct = totalAssignments == 0 ? 0.0 : (completed * 100.0 / totalAssignments);

        double avgScorePct = submissions.stream()
                .filter(s -> s.getScore() != null && s.getAssignment().getMaxScore() != null
                        && s.getAssignment().getMaxScore() > 0)
                .mapToDouble(s -> (s.getScore() * 100.0) / s.getAssignment().getMaxScore())
                .average()
                .orElse(0.0);

        return new Progress((int) completed, completionPct, avgScorePct);
    }

    // Simple carrier for the two progress figures plus the completed count.
    private static class Progress {
        final int completed;
        final double completionPct;
        final double avgScorePct;
        Progress(int completed, double completionPct, double avgScorePct) {
            this.completed = completed;
            this.completionPct = completionPct;
            this.avgScorePct = avgScorePct;
        }
    }

    // ---- Training Batches (ADMIN) ----

    // Roll-up of every training batch for the admin Training Batches overview.
    // A batch is the Intern.batch String, so figures are aggregated from the
    // interns in each cohort — no Batch entity exists or is needed.
    @GET
    @Path("/batches")
    @RolesAllowed("ADMIN")
    public List<BatchSummary> getBatchSummaries() {
        List<Assignment> allAssignments = assignmentRepository.findAll();
        int totalAssignments = allAssignments.size();

        List<BatchSummary> summaries = new ArrayList<>();
        for (String batch : internRepository.findDistinctBatches()) {
            List<Intern> interns = internRepository.findByBatch(batch);

            List<String> tracks = interns.stream()
                    .map(Intern::getTrack)
                    .filter(t -> t != null && !t.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            // Distinct training names across the batch's interns. Trainings are
            // assigned per batch, so the distinct count is how many trainings
            // this cohort has.
            Set<String> trainingNames = new HashSet<>();
            double sumCompletion = 0.0;
            double sumScore = 0.0;
            for (Intern intern : interns) {
                trainingRepository.findByInternId(intern.getId()).forEach(t -> {
                    if (t.getTrainingName() != null && !t.getTrainingName().isBlank()) {
                        trainingNames.add(t.getTrainingName().trim().toLowerCase());
                    }
                });
                Progress p = computeProgress(intern.getId(), totalAssignments);
                sumCompletion += p.completionPct;
                sumScore += p.avgScorePct;
            }

            long assignmentCount = allAssignments.stream()
                    .filter(a -> batch.equals(a.getBatch()))
                    .count();

            int n = interns.size();
            double avgCompletion = n == 0 ? 0.0 : sumCompletion / n;
            double avgScore = n == 0 ? 0.0 : sumScore / n;

            summaries.add(new BatchSummary(batch, n, tracks, trainingNames.size(),
                    (int) assignmentCount, round2(avgCompletion), round2(avgScore)));
        }
        return summaries;
    }

    // Rename a batch: move every intern in the cohort to the new batch string,
    // and keep any assignments that target that batch in sync. A batch is just a
    // String, so this is a bulk value update — no schema change.
    @PUT
    @Path("/batches/rename")
    @RolesAllowed("ADMIN")
    public Response renameBatch(RenameBatchRequest req) {
        if (req == null || req.oldBatch == null || req.oldBatch.isBlank()
                || req.newBatch == null || req.newBatch.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Both oldBatch and newBatch are required.\"}").build();
        }

        String oldBatch = req.oldBatch.trim();
        String newBatch = req.newBatch.trim();
        if (oldBatch.equals(newBatch)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"New batch name must differ from the current one.\"}").build();
        }

        List<Intern> interns = internRepository.findByBatch(oldBatch);
        if (interns.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"No interns found in batch \\\"" + oldBatch + "\\\".\"}").build();
        }

        for (Intern intern : interns) {
            intern.setBatch(newBatch);
            internRepository.save(intern);
        }

        // AssignmentRepository has no findByBatch, so scan and update the ones
        // that point at the old batch to keep the cohort's assignments consistent.
        int assignmentsUpdated = 0;
        for (Assignment a : assignmentRepository.findAll()) {
            if (oldBatch.equals(a.getBatch())) {
                a.setBatch(newBatch);
                assignmentRepository.save(a);
                assignmentsUpdated++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("oldBatch", oldBatch);
        result.put("newBatch", newBatch);
        result.put("internsUpdated", interns.size());
        result.put("assignmentsUpdated", assignmentsUpdated);
        return Response.ok(result).build();
    }

    // Archive an entire batch to one non-active status (ADMIN). Records are never
    // deleted — every intern in the cohort simply moves to the chosen status.
    // ACTIVE is rejected here: reactivation is a per-intern action via /status.
    @POST
    @Path("/batch/archive")
    @RolesAllowed("ADMIN")
    public Response archiveBatch(BatchArchiveRequest req) {
        if (req == null || req.batch == null || req.batch.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Batch is required.\"}").build();
        }
        if (req.status == null || req.status.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Status is required.\"}").build();
        }
        Intern.Status status = parseStatus(req.status);
        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid status \\\"" + req.status + "\\\".\"}").build();
        }
        if (status == Intern.Status.ACTIVE) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Batch archive cannot set interns to ACTIVE.\"}").build();
        }

        String batch = req.batch.trim();
        List<Intern> interns = internRepository.findByBatch(batch);
        if (interns.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"No interns found in batch \\\"" + batch + "\\\".\"}").build();
        }

        for (Intern intern : interns) {
            intern.setStatus(status);
            internRepository.save(intern);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("status", status.name());
        result.put("internsUpdated", interns.size());
        return Response.ok(result).build();
    }

    // ---- Training History ----

    // View an intern's trainings and their GitHub links (ADMIN + TRAINER).
    @GET
    @Path("/{id}/trainings")
    public Response getTrainings(@PathParam("id") Long internId) {
        Intern intern = internRepository.findById(internId);
        if (intern == null || !canView(intern)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Intern not found").build();
        }
        return Response.ok(trainingRepository.findByInternId(internId)).build();
    }

    // Distinct training names in use, for the "assign an assignment to a
    // training" dropdown. Independent assignments simply pick none.
    @GET
    @Path("/trainings/names")
    public List<String> getTrainingNames() {
        return trainingRepository.findDistinctTrainingNames();
    }

    // Assign one training to every intern in a batch at once (ADMIN).
    // Interns who already have a training with the same name are skipped so
    // re-running the assignment is safe. Returns how many were added vs skipped.
    @POST
    @Path("/trainings/batch")
    @RolesAllowed("ADMIN")
    public Response assignTrainingToBatch(BatchTrainingRequest req) {
        if (req == null || req.trainingName == null || req.trainingName.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Training name is required.\"}").build();
        }
        if (req.batch == null || req.batch.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Batch is required.\"}").build();
        }

        String trainingName = req.trainingName.trim();
        String repoUrl = normalizeRepoUrl(req.repoUrl);
        String batch = req.batch.trim();

        List<Intern> interns = internRepository.findByBatch(batch);
        int assigned = 0;
        int skipped = 0;
        for (Intern intern : interns) {
            boolean alreadyHas = trainingRepository.findByInternId(intern.getId()).stream()
                    .anyMatch(t -> t.getTrainingName() != null
                            && t.getTrainingName().equalsIgnoreCase(trainingName));
            if (alreadyHas) {
                skipped++;
                continue;
            }
            trainingRepository.save(new Training(intern, trainingName, repoUrl));
            assigned++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("trainingName", trainingName);
        result.put("total", interns.size());
        result.put("assigned", assigned);
        result.put("skipped", skipped);
        return Response.ok(result).build();
    }

    // Edit a training's name (ADMIN). The Box Drive link is owned by the
    // training at batch-assign time, so it is intentionally left untouched here.
    @PUT
    @Path("/{id}/trainings/{trainingId}")
    @RolesAllowed("ADMIN")
    public Response updateTraining(@PathParam("id") Long internId,
                                   @PathParam("trainingId") Long trainingId,
                                   TrainingRequest req) {
        Training existing = trainingRepository.findById(trainingId);
        if (existing == null || existing.getIntern() == null
                || !existing.getIntern().getId().equals(internId)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Training not found").build();
        }
        if (req == null || req.trainingName == null || req.trainingName.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Training name is required.\"}").build();
        }
        existing.setTrainingName(req.trainingName.trim());
        return Response.ok(trainingRepository.save(existing)).build();
    }

    // Remove a training from an intern (ADMIN).
    @DELETE
    @Path("/{id}/trainings/{trainingId}")
    @RolesAllowed("ADMIN")
    public Response deleteTraining(@PathParam("id") Long internId,
                                   @PathParam("trainingId") Long trainingId) {
        Training existing = trainingRepository.findById(trainingId);
        if (existing == null || existing.getIntern() == null
                || !existing.getIntern().getId().equals(internId)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Training not found").build();
        }
        trainingRepository.deleteById(trainingId);
        return Response.noContent().build();
    }

    // Treat blank as "no repository" so the UI's optional field stays clean.
    private String normalizeRepoUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.trim();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // Small JSON error response helper, matching the inline style used elsewhere
    // in this resource.
    private Response error(Response.Status status, String message) {
        return Response.status(status).entity("{\"error\":\"" + message + "\"}").build();
    }

    // Trim a text field and collapse empty/blank input to null so cleared form
    // fields do not persist as empty strings.
    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Total hours must be zero or positive; a negative value is treated as unset
    // rather than stored, so the field can never go below zero.
    private Double sanitizeHours(Double hours) {
        if (hours == null || hours < 0) {
            return null;
        }
        return hours;
    }

    // Parse a status name to the enum, tolerating surrounding whitespace and case.
    // Returns null for an unknown value so callers can reject with a 400.
    private Intern.Status parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Intern.Status.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
