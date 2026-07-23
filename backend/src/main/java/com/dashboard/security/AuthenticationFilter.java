package com.dashboard.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;

/**
 * Authenticates requests to @Secured endpoints by validating the bearer token
 * and installing a SecurityContext (used later by AuthorizationFilter and by
 * resources that inject @Context SecurityContext).
 *
 * Returns 401 if the token is missing or invalid.
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String BEARER = "Bearer ";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Never block CORS preflight requests.
        if (HttpMethod.OPTIONS.equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            abort(requestContext);
            return;
        }

        String token = authHeader.substring(BEARER.length()).trim();
        TokenService.Claims claims = TokenService.validate(token);
        if (claims == null) {
            abort(requestContext);
            return;
        }

        final boolean secure = requestContext.getSecurityContext().isSecure();
        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> claims.username;
            }

            @Override
            public boolean isUserInRole(String role) {
                return claims.role.name().equals(role);
            }

            @Override
            public boolean isSecure() {
                return secure;
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        });
    }

    private void abort(ContainerRequestContext requestContext) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Unauthorized\"}")
                        .type("application/json")
                        .build());
    }
}
