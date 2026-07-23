package com.dashboard;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
public class JaxRsActivator extends Application {
    // No overrides needed - JAX-RS auto-discovers @Path-annotated classes
}
