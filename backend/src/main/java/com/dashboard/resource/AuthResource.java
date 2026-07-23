package com.dashboard.resource;

import com.dashboard.dto.LoginRequest;
import com.dashboard.dto.LoginResponse;
import com.dashboard.entity.User;
import com.dashboard.repository.UserRepository;
import com.dashboard.security.PasswordUtil;
import com.dashboard.security.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Public authentication endpoint. Deliberately NOT @Secured so that unauthenticated
 * clients can obtain a token.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private UserRepository userRepository;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null
                || request.username == null || request.username.isBlank()
                || request.password == null || request.password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username and password are required.\"}")
                    .build();
        }

        User user = userRepository.findByUsername(request.username.trim());
        // Same response for unknown user and wrong password (avoid user enumeration).
        if (user == null || !PasswordUtil.verify(request.password, user.getPasswordHash())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid username or password.\"}")
                    .build();
        }

        // Disabled accounts have valid credentials but must not be allowed to log in.
        if (!user.isEnabled()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Your account has been disabled. Contact your administrator.\"}")
                    .build();
        }

        String token = TokenService.issue(user);
        LoginResponse body = new LoginResponse(
                token, user.getUsername(), user.getRole().name(), user.getFullName());
        return Response.ok(body).build();
    }
}
