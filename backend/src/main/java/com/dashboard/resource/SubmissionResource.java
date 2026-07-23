package com.dashboard.resource;

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

@Path("/submissions")
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

        Submission.Status status = req.status != null
                ? Submission.Status.valueOf(req.status)
                : Submission.Status.GRADED;

        Submission submission = new Submission(intern, assignment, req.score, status);
        return Response.status(Response.Status.CREATED)
                .entity(submissionRepository.save(submission)).build();
    }
}