package com.dashboard.resource;

import com.dashboard.entity.Feedback;
import com.dashboard.entity.Intern;
import com.dashboard.repository.FeedbackRepository;
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

import java.util.List;

/**
 * Teacher feedback on an intern. Newest-first list per intern, plus create/edit/delete.
 *
 * Authorization mirrors the rest of the app and is enforced here — never only in the UI:
 *  - ADMIN can view/edit/delete all feedback, including feedback on archived interns.
 *  - TRAINER can only reach ACTIVE interns (canView) — archived interns and their
 *    feedback 404 for trainers, so a direct API call cannot leak them.
 *  - TRAINER may edit/delete only feedback they authored (compared by authorUsername,
 *    the stable login, not the display name). Editing/deleting another trainer's
 *    feedback is 403.
 *  - Feedback is only ever removed by an explicit authorized DELETE.
 */
@Path("/feedback")
@Secured
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeedbackResource {

    // Matches the entity column length; keep the API error in step with the DB cap.
    private static final int MAX_CONTENT_LENGTH = 4000;

    @Inject
    private FeedbackRepository feedbackRepository;
    @Inject
    private InternRepository internRepository;
    @Inject
    private UserRepository userRepository;

    @Context
    private SecurityContext securityContext;

    private boolean isAdmin() {
        return securityContext.isUserInRole("ADMIN");
    }

    // Admins see every intern; trainers only ACTIVE ones (legacy NULL counts as
    // active). Enforced here so a trainer cannot reach an archived record's
    // feedback by calling the API directly.
    private boolean canView(Intern intern) {
        return isAdmin() || intern.getStatus() == Intern.Status.ACTIVE;
    }

    // Stable login of the caller, used for ownership checks. Null if unauthenticated
    // (should not happen behind @Secured, but guarded for safety).
    private String currentUsername() {
        return securityContext.getUserPrincipal() == null
                ? null : securityContext.getUserPrincipal().getName();
    }

    // Display name captured on write: the user's full name when set, else the login.
    private String recorderName() {
        String username = currentUsername();
        if (username == null) {
            return null;
        }
        var user = userRepository.findByUsername(username);
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return username;
    }

    // List one intern's feedback, newest first. Trainers 404 on archived interns.
    @GET
    public Response list(@QueryParam("internId") Long internId) {
        if (internId == null) {
            return error(Response.Status.BAD_REQUEST, "internId is required.");
        }
        Intern intern = internRepository.findById(internId);
        if (intern == null || !canView(intern)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(feedbackRepository.findByInternId(internId)).build();
    }

    // Create feedback on an intern. ADMIN or TRAINER; trainers only on ACTIVE interns.
    @POST
    public Response create(FeedbackRequest req) {
        if (req == null || req.internId == null) {
            return error(Response.Status.BAD_REQUEST, "internId is required.");
        }
        String content = req.content == null ? "" : req.content.trim();
        if (content.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Feedback content is required.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            return error(Response.Status.BAD_REQUEST,
                    "Feedback must be at most " + MAX_CONTENT_LENGTH + " characters.");
        }
        Intern intern = internRepository.findById(req.internId);
        if (intern == null || !canView(intern)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Feedback saved = feedbackRepository.save(
                new Feedback(intern, currentUsername(), recorderName(), content));
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    // Edit feedback content. Admins any; trainers only their own and only on
    // interns they can still view.
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, FeedbackRequest req) {
        Feedback existing = feedbackRepository.findById(id);
        if (existing == null || existing.getIntern() == null
                || !canView(existing.getIntern())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Response denied = requireOwnership(existing);
        if (denied != null) {
            return denied;
        }
        String content = req == null || req.content == null ? "" : req.content.trim();
        if (content.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Feedback content is required.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            return error(Response.Status.BAD_REQUEST,
                    "Feedback must be at most " + MAX_CONTENT_LENGTH + " characters.");
        }
        existing.setContent(content);
        // merge triggers @PreUpdate, stamping updatedAt so the UI can show "edited".
        return Response.ok(feedbackRepository.save(existing)).build();
    }

    // Delete feedback. Admins any (incl. archived interns); trainers only their own.
    // Feedback is never removed except through this explicit authorized call.
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        Feedback existing = feedbackRepository.findById(id);
        if (existing == null || existing.getIntern() == null
                || !canView(existing.getIntern())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Response denied = requireOwnership(existing);
        if (denied != null) {
            return denied;
        }
        feedbackRepository.deleteById(id);
        return Response.noContent().build();
    }

    // Ownership gate: admins bypass; a trainer may only touch feedback whose
    // authorUsername matches their login. Returns a 403 Response when denied,
    // or null when the caller is allowed to proceed.
    private Response requireOwnership(Feedback feedback) {
        if (isAdmin()) {
            return null;
        }
        String username = currentUsername();
        if (username == null || !username.equals(feedback.getAuthorUsername())) {
            return error(Response.Status.FORBIDDEN,
                    "You can only modify feedback you wrote.");
        }
        return null;
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status).entity("{\"error\":\"" + message + "\"}").build();
    }

    // Inline request DTO (public-field convention shared with the other resources).
    public static class FeedbackRequest {
        public Long internId;
        public String content;
    }
}
