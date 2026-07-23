package com.dashboard.resource;

import com.dashboard.dto.BatchTrainingRequest;
import com.dashboard.dto.ProgressSummary;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        List<Assignment> allAssignments = assignmentRepository.findAll();
        List<Submission> submissions = submissionRepository.findByInternId(internId);

        int total = allAssignments.size();
        long completed = submissions.stream()
                .filter(s -> s.getStatus() == Submission.Status.GRADED
                        || s.getStatus() == Submission.Status.SUBMITTED)
                .count();

        double completionPct = total == 0 ? 0.0 : (completed * 100.0 / total);

        double avgScorePct = submissions.stream()
                .filter(s -> s.getScore() != null && s.getAssignment().getMaxScore() != null
                        && s.getAssignment().getMaxScore() > 0)
                .mapToDouble(s -> (s.getScore() * 100.0) / s.getAssignment().getMaxScore())
                .average()
                .orElse(0.0);

        ProgressSummary summary = new ProgressSummary(intern.getId(), intern.getName(), total,
                (int) completed, round2(completionPct), round2(avgScorePct));
        return Response.ok(summary).build();
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