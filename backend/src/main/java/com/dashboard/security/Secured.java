package com.dashboard.security;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JAX-RS resource class or method as requiring authentication.
 * Endpoints annotated with @Secured are processed by AuthenticationFilter
 * (and AuthorizationFilter, which enforces any @RolesAllowed constraints).
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured {
}
