package com.example.cxfdemo.rest;

import java.time.OffsetDateTime;
import java.util.Map;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    @GET
    public Map<String, String> health() {
        log.info("HealthResource.health checked");
        Map<String, String> map = new java.util.HashMap<>();
        map.put("status", "UP");
        map.put("service", "ssm-cxf-webservice-demo");
        map.put("time", OffsetDateTime.now().toString());
        return map;
    }
}
