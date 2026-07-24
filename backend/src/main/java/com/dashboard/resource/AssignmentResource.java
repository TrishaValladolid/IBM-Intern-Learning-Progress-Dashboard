package com.dashboard.resource;

import com.dashboard.entity.Assignment;
import com.dashboard.repository.AssignmentRepository;
import com.dashboard.repository.UserRepository;
import com.dashboard.entity.User;
import com.dashboard.security.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/assignments")
@Secured
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssignmentResource {

    @Inject
    private AssignmentRepository assignmentRepository;
    @Inject
    private UserRepository userRepository;
    @Context
    private SecurityContext securityContext;

    @GET
    public List<Assignment> getAll() {
        List<Assignment> all = assignmentRepository.findAll();
        if (securityContext.isUserInRole("ADMIN")) return all;
        User trainer = securityContext.getUserPrincipal() == null ? null
                : userRepository.findByUsername(securityContext.getUserPrincipal().getName());
        if (trainer == null || trainer.getAssignedTrainings().isEmpty()) return all;
        return all.stream().filter(a -> a.getTrainingName() != null
                && trainer.getAssignedTrainings().contains(a.getTrainingName().trim())).toList();
    }

    @POST
    public Response create(Assignment assignment) {
        Assignment saved = assignmentRepository.save(assignment);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Assignment updated) {
        Assignment existing = assignmentRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setTitle(updated.getTitle());
        existing.setMaxScore(updated.getMaxScore());
        existing.setBatch(updated.getBatch());
        existing.setTrainingName(updated.getTrainingName());
        existing.setRepoUrl(updated.getRepoUrl());
        existing.setDueDate(updated.getDueDate());
        return Response.ok(assignmentRepository.save(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        assignmentRepository.deleteById(id);
        return Response.noContent().build();
    }
}
