package com.example.appa;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.utils.GenericDao;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

class MainNouternalResourceIntegrationTest {

    @Test
    void restWritesMainDaoExternalJarDaoAndAuditMapperXml() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AppAApplication.class)
                .web(WebApplicationType.SERVLET)
                .run("--spring.main.banner-mode=off")) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            String suffix = Long.toString(System.nanoTime());
            String policyNo = "TEST-" + suffix;
            String customerName = "External-" + suffix;
            String requestId = "request-" + suffix;
            String body = """
                    {
                      "policyNo":"%s",
                      "holderName":"Integration Test",
                      "productName":"CXF",
                      "status":"ACTIVE",
                      "customerName":"%s"
                    }
                    """.formatted(policyNo, customerName);

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://localhost:" + port + "/rest/mainNouternal/dao"))
                            .header("Content-Type", "application/json")
                            .header("X-Request-Id", requestId)
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body()).contains(policyNo, customerName, "\"externalCommitted\":true");

            try (Connection connection = GenericDao.getConnection1();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT holder_name FROM policy_info WHERE policy_no = ?")) {
                statement.setString(1, policyNo);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString(1)).isEqualTo("Integration Test");
                }
            }

            try (Connection connection = com.external.liba.utils.GenericDao.getConnection1();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT status FROM customers WHERE customer_name = ?")) {
                statement.setString(1, customerName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString(1)).isEqualTo("ACTIVE");
                }
            }

            try (Connection connection = GenericDao.getConnection1();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT response_code FROM audit_response WHERE guid = ?")) {
                statement.setString(1, requestId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getInt(1)).isEqualTo(201);
                }
            }
        }
    }
}
