package com.dashboard.security;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.lang.reflect.Method;

/**
 * Enforces role-based access on @Secured endpoints by reading @RolesAllowed
 * (or @PermitAll / @DenyAll) on the target method, falling back to the class.
 * Runs after AuthenticationFilter, so a SecurityContext is already installed.
 *
 * Returns 403 when the authenticated user's role is not permitted.
 */
@Secured
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (HttpMethod.OPTIONS.equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        Method method = resourceInfo.getResourceMethod();
        Class<?> resourceClass = resourceInfo.getResourceClass();

        // Method-level annotations take precedence over class-level.
        if (method.isAnnotationPresent(DenyAll.class)) {
            deny(requestContext);
            return;
        }
        if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }
        RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            if (resourceClass.isAnnotationPresent(DenyAll.class)) {
                deny(requestContext);
                return;
            }
            if (resourceClass.isAnnotationPresent(PermitAll.class)) {
                return;
            }
            rolesAllowed = resourceClass.getAnnotation(RolesAllowed.class);
        }

        // No @RolesAllowed anywhere: any authenticated user may proceed.
        if (rolesAllowed == null) {
            return;
        }

        SecurityContext securityContext = requestContext.getSecurityContext();
        for (String role : rolesAllowed.value()) {
            if (securityContext.isUserInRole(role)) {
                return;
            }
        }
        deny(requestContext);
    }

    private void deny(ContainerRequestContext requestContext) {
        requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"Forbidden\"}")
                        .type("application/json")
                        .build());
    }
}
