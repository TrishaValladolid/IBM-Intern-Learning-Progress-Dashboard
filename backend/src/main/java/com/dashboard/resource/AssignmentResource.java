package com.dashboard.resource;

import com.dashboard.entity.Assignment;
import com.dashboard.repository.AssignmentRepository;
import com.dashboard.security.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/assignments")
@Secured
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssignmentResource {

    @Inject
    private AssignmentRepository assignmentRepository;

    @GET
    public List<Assignment> getAll() {
        return assignmentRepository.findAll();
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
        return Response.ok(assignmentRepository.save(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        assignmentRepository.deleteById(id);
        return Response.noContent().build();
    }
}
