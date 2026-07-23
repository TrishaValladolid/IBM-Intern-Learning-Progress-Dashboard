package com.dashboard.resource;

import com.dashboard.dto.CreateTrainerRequest;
import com.dashboard.dto.PasswordResetRequest;
import com.dashboard.dto.StatusRequest;
import com.dashboard.dto.UpdateTrainerRequest;
import com.dashboard.dto.UserResponse;
import com.dashboard.entity.User;
import com.dashboard.repository.UserRepository;
import com.dashboard.security.PasswordUtil;
import com.dashboard.security.Secured;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Trainer Management. Restricted to ADMIN: the class-level @Secured installs the
 * security context and every method requires the ADMIN role, so TRAINER tokens are
 * rejected with 403 before any handler runs. Reuses the existing User entity,
 * UserRepository and PasswordUtil — no separate trainer entity or hashing scheme.
 */
@Path("/trainers")
@Secured
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TrainerResource {

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Inject
    private UserRepository userRepository;

    @GET
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response getOne(@PathParam("id") Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(UserResponse.from(user)).build();
    }

    @POST
    public Response create(CreateTrainerRequest req) {
        if (req == null) {
            return error(Response.Status.BAD_REQUEST, "Request body is required.");
        }
        String firstName = trim(req.firstName);
        String lastName = trim(req.lastName);
        String username = trim(req.username);
        String email = trim(req.email);

        if (firstName.isEmpty() || lastName.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "First name and last name are required.");
        }
        if (username.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Username is required.");
        }
        if (req.password == null || req.password.length() < MIN_PASSWORD_LENGTH) {
            return error(Response.Status.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!req.password.equals(req.confirmPassword)) {
            return error(Response.Status.BAD_REQUEST, "Passwords do not match.");
        }
        if (userRepository.existsByUsername(username)) {
            return error(Response.Status.CONFLICT, "That username is already taken.");
        }

        User user = new User();
        user.setUsername(username);
        user.setFullName((firstName + " " + lastName).trim());
        user.setEmail(email.isEmpty() ? null : email);
        user.setPasswordHash(PasswordUtil.hash(req.password));
        user.setRole(parseRole(req.role));
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return Response.status(Response.Status.CREATED).entity(UserResponse.from(saved)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, UpdateTrainerRequest req) {
        User existing = userRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (req == null) {
            return error(Response.Status.BAD_REQUEST, "Request body is required.");
        }
        String username = trim(req.username);
        String fullName = trim(req.fullName);
        String email = trim(req.email);

        if (fullName.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Full name is required.");
        }
        if (username.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Username is required.");
        }
        // Only check uniqueness if the username actually changed.
        if (!username.equalsIgnoreCase(existing.getUsername())
                && userRepository.existsByUsername(username)) {
            return error(Response.Status.CONFLICT, "That username is already taken.");
        }

        existing.setFullName(fullName);
        existing.setUsername(username);
        existing.setEmail(email.isEmpty() ? null : email);
        return Response.ok(UserResponse.from(userRepository.save(existing))).build();
    }

    @PUT
    @Path("/{id}/password")
    public Response resetPassword(@PathParam("id") Long id, PasswordResetRequest req) {
        User existing = userRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (req == null || req.newPassword == null || req.newPassword.length() < MIN_PASSWORD_LENGTH) {
            return error(Response.Status.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!req.newPassword.equals(req.confirmPassword)) {
            return error(Response.Status.BAD_REQUEST, "Passwords do not match.");
        }
        existing.setPasswordHash(PasswordUtil.hash(req.newPassword));
        userRepository.save(existing);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/status")
    public Response setStatus(@PathParam("id") Long id, StatusRequest req,
                              @Context SecurityContext securityContext) {
        User existing = userRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (req == null) {
            return error(Response.Status.BAD_REQUEST, "Request body is required.");
        }
        if (!req.enabled && isCurrentUser(existing, securityContext)) {
            return error(Response.Status.CONFLICT, "You cannot disable your own account.");
        }
        existing.setEnabled(req.enabled);
        return Response.ok(UserResponse.from(userRepository.save(existing))).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        User existing = userRepository.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (isCurrentUser(existing, securityContext)) {
            return error(Response.Status.CONFLICT, "You cannot delete your own account.");
        }
        userRepository.deleteById(id);
        return Response.noContent().build();
    }

    private boolean isCurrentUser(User user, SecurityContext securityContext) {
        return securityContext.getUserPrincipal() != null
                && user.getUsername() != null
                && user.getUsername().equals(securityContext.getUserPrincipal().getName());
    }

    private User.Role parseRole(String role) {
        if (role != null) {
            try {
                return User.Role.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to default
            }
        }
        return User.Role.TRAINER;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity("{\"error\":\"" + message + "\"}")
                .build();
    }
}
