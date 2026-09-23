package com.example.cxfdemo.rest;

import com.example.cxfdemo.dao.PolicyDaoImpl;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/** Test-only resource: no @Bean, @Import, or entry in a CXF service list. */
@Path("/scan-proof")
@Produces(MediaType.APPLICATION_JSON)
public class ScannedProbeResource {
    private final PolicyDaoImpl policyDao;

    public ScannedProbeResource(PolicyDaoImpl policyDao) {
        this.policyDao = policyDao;
    }

    @GET
    public Map<String, String> readMainDatabase() {
        return Map.of("message", policyDao.getJdbcTemplate().queryForObject(
                "SELECT prop_value FROM system_properties WHERE prop_key = 'system.app.message'",
                String.class));
    }
}
