package com.example.appa.openapi;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.springframework.stereotype.Component;

// Swagger UI 5 keeps its specification URL in this script. This route replaces the
// WebJar's Petstore default for app-a's existing /rest/* CXF servlet mapping.
@Hidden
@Component
@Path("/api-docs/swagger-initializer.js")
public class SwaggerUiInitializerResource {

    private static final String INITIALIZER = """
            window.onload = function() {
              window.ui = SwaggerUIBundle({
                url: "/rest/openapi.json",
                dom_id: '#swagger-ui',
                deepLinking: true,
                tryItOutEnabled: true,
                presets: [
                  SwaggerUIBundle.presets.apis,
                  SwaggerUIStandalonePreset
                ],
                plugins: [
                  SwaggerUIBundle.plugins.DownloadUrl
                ],
                layout: "StandaloneLayout"
              });
            };
            """;

    @GET
    @Produces("application/javascript")
    public String initializer() {
        return INITIALIZER;
    }
}
