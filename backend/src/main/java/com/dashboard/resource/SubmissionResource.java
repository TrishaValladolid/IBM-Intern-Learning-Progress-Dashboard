package com.dashboard.resource;

import com.dashboard.dto.GradeCell;
import com.dashboard.entity.Assignment;
import com.dashboard.entity.Intern;
import com.dashboard.entity.Submission;
import com.dashboard.repository.AssignmentRepository;
import com.dashboard.repository.InternRepository;
import com.dashboard.repository.SubmissionRepository;
import com.dashboard.security.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

@Path("/submissions")
@Secured
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubmissionResource {

    @Inject
    private SubmissionRepository submissionRepository;
    @Inject
    private InternRepository internRepository;
    @Inject
    private AssignmentRepository assignmentRepository;

    public static class SubmissionRequest {
        public Long internId;
        public Long assignmentId;
        public Integer score;
        public String status; // "PENDING" | "SUBMITTED" | "GRADED"
    }

    // Flat grade list powering the Assignments and Grades matrix: one entry per
    // recorded score (intern x assignment). The frontend keys these by
    // internId-assignmentId to fill the grid.
    @GET
    public List<GradeCell> getAllGrades() {
        return submissionRepository.findAll().stream()
                .map(s -> new GradeCell(s.getIntern().getId(),
                        s.getAssignment().getId(), s.getScore()))
                .collect(Collectors.toList());
    }

    @POST
    public Response recordSubmission(SubmissionRequest req) {
        Intern intern = internRepository.findById(req.internId);
        if (intern == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Intern not found").build();
        }
        Assignment assignment = assignmentRepository.findById(req.assignmentId);
        if (assignment == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Assignment not found").build();
        }

        // Range check: a score must fall within 0..maxScore for the assignment.
        // Guards against recording e.g. 100 on a 50-point assignment.
        if (req.score != null) {
            Integer maxScore = assignment.getMaxScore();
            if (req.score < 0 || (maxScore != null && req.score > maxScore)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Score must be between 0 and " + maxScore + ".")
                        .build();
            }
        }

        Submission.Status status = req.status != null
                ? Submission.Status.valueOf(req.status)
                : Submission.Status.GRADED;

        // Upsert: one grade per intern+assignment. Re-grading updates the
        // existing row in place instead of inserting a duplicate, which would
        // otherwise skew the matrix and the profile's average/completion math.
        Submission existing = submissionRepository
                .findByInternIdAndAssignmentId(req.internId, req.assignmentId);
        if (existing != null) {
            existing.setScore(req.score);
            existing.setStatus(status);
            Submission saved = submissionRepository.save(existing);
            // Return a flat DTO, not the entity: serializing the lazy
            // intern/assignment relations would trigger a JSON-B error.
            return Response.ok(new GradeCell(req.internId, req.assignmentId, saved.getScore())).build();
        }

        Submission submission = new Submission(intern, assignment, req.score, status);
        Submission saved = submissionRepository.save(submission);
        return Response.status(Response.Status.CREATED)
                .entity(new GradeCell(intern.getId(), assignment.getId(), saved.getScore())).build();
    }
}