package com.dashboard.resource;

import com.dashboard.dto.BatchSummary;
import com.dashboard.dto.BatchTrainingRequest;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    @GET
    public List<Intern> getAll() {
        return internRepository.findAll();
    }

    @GET
    @Path("/{id}")
    public Response getOne(@PathParam("id") Long id) {
        Intern intern = internRepository.findById(id);
        if (intern == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(intern).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response create(Intern intern) {
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
    public List<Submission> getSubmissionsForIntern(@PathParam("id") Long internId) {
        return submissionRepository.findByInternId(internId);
    }

    // Main feature: overall learning progress for one intern
    @GET
    @Path("/{id}/progress")
    public Response getProgress(@PathParam("id") Long internId) {
        Intern intern = internRepository.findById(internId);
        if (intern == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Intern not found").build();
        }

        int total = assignmentRepository.findAll().size();
        Progress p = computeProgress(internId, total);

        ProgressSummary summary = new ProgressSummary(intern.getId(), intern.getName(), total,
                p.completed, round2(p.completionPct), round2(p.avgScorePct));
        return Response.ok(summary).build();
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

    // ---- Training History ----

    // View an intern's trainings and their GitHub links (ADMIN + TRAINER).
    @GET
    @Path("/{id}/trainings")
    public Response getTrainings(@PathParam("id") Long internId) {
        if (internRepository.findById(internId) == null) {
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
}