package com.dashboard.resource;

import com.dashboard.dto.ProgressSummary;
import com.dashboard.entity.Assignment;
import com.dashboard.entity.Intern;
import com.dashboard.entity.Submission;
import com.dashboard.repository.AssignmentRepository;
import com.dashboard.repository.InternRepository;
import com.dashboard.repository.SubmissionRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/interns")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternResource {

    @Inject
    private InternRepository internRepository;
    @Inject
    private AssignmentRepository assignmentRepository;
    @Inject
    private SubmissionRepository submissionRepository;

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
    public Response create(Intern intern) {
        Intern saved = internRepository.save(intern);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Intern updated) {
        Intern existing = internRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setName(updated.getName());
        existing.setEmployeeId(updated.getEmployeeId());
        existing.setBatch(updated.getBatch());
        existing.setTrack(updated.getTrack());
        return Response.ok(internRepository.save(existing)).build();
    }

    @DELETE
    @Path("/{id}")
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

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}