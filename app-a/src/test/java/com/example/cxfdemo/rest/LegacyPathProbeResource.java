package com.example.cxfdemo.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/** Test-only proof that one root JAX-RS server serves all legacy path shapes. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class LegacyPathProbeResource {

    @GET
    @Path("savxxx/probe")
    public Map<String, String> savxxx() {
        return Map.of("path", "savxxx");
    }

    @GET
    @Path("ctbcxxxx/probe")
    public Map<String, String> ctbcxxxx() {
        return Map.of("path", "ctbcxxxx");
    }

    @GET
    @Path("xxx/probe")
    public Map<String, String> xxx() {
        return Map.of("path", "xxx");
    }
}
