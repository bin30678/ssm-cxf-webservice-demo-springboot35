package com.example.appa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.cxfdemo.dao.AuditLogDao;
import jakarta.xml.ws.Endpoint;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

class CxfAuditInterceptorIntegrationTest {

    @Test
    void sharedBusInterceptorsApplyToRestAndSoapEndpoints() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                AppAApplication.class, AuditProbeConfiguration.class)
                .web(WebApplicationType.SERVLET)
                .run("--server.port=0", "--spring.main.banner-mode=off")) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> restResponse = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/rest/health"))
                            .header("X-Request-Id", "rest-audit-guid")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertThat(restResponse.statusCode()).isEqualTo(200);
            assertThat(restResponse.body()).contains("\"status\":\"UP\"");
            assertAuditHeaders(restResponse, "rest-audit-guid");

            HttpResponse<String> restFaultResponse = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/rest/health"))
                            .header("X-Request-Id", "rest-fault-guid")
                            .header("Content-Type", "text/plain")
                            .POST(HttpRequest.BodyPublishers.ofString("unsupported"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertThat(restFaultResponse.statusCode()).isEqualTo(415);
            assertAuditHeaders(restFaultResponse, "rest-fault-guid");

            String soapEnvelope = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                                   xmlns:a="https://example.com/app-a/audit">
                      <soap:Body>
                        <a:ping><value>SOAP</value></a:ping>
                      </soap:Body>
                    </soap:Envelope>
                    """;
            HttpResponse<String> soapResponse = client.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://localhost:" + port + "/Webservice/soap/audit"))
                            .header("X-Request-Id", "soap-audit-guid")
                            .header("Content-Type", "text/xml; charset=UTF-8")
                            .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope, StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertThat(soapResponse.statusCode()).isEqualTo(200);
            assertThat(soapResponse.body()).contains("PONG:SOAP");
            assertAuditHeaders(soapResponse, "soap-audit-guid");

            String soapFaultEnvelope = soapEnvelope.replace(">SOAP<", ">FAIL<");
            HttpResponse<String> soapFaultResponse = client.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://localhost:" + port + "/Webservice/soap/audit"))
                            .header("X-Request-Id", "soap-fault-guid")
                            .header("Content-Type", "text/xml; charset=UTF-8")
                            .POST(HttpRequest.BodyPublishers.ofString(soapFaultEnvelope, StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            assertThat(soapFaultResponse.statusCode()).isEqualTo(500);
            assertThat(soapFaultResponse.body())
                    .contains("PROBE_ERROR")
                    .contains("SOAP 測試錯誤")
                    .contains("soap-fault-guid");
            assertAuditHeaders(soapFaultResponse, "soap-fault-guid");

            AuditLogDao auditLogDao = context.getBean(AuditLogDao.class);
            verify(auditLogDao).insertRequestLog(
                    eq("rest-audit-guid"), any(), any(), anyString(), eq("/rest/health"), eq("GET"),
                    nullable(String.class));
            verify(auditLogDao).insertResponseLog(eq("rest-audit-guid"), eq(200), contains("\"status\":\"UP\""));
            verify(auditLogDao).insertFaultLog(eq("rest-fault-guid"), anyString());
            verify(auditLogDao).insertRequestLog(
                    eq("soap-audit-guid"), any(), any(), anyString(), eq("/Webservice/soap/audit"), eq("POST"),
                    contains("<a:ping>"));
            verify(auditLogDao).insertResponseLog(eq("soap-audit-guid"), eq(200), contains("PONG:SOAP"));
            verify(auditLogDao).insertFaultLog(eq("soap-fault-guid"), anyString());
        }
    }

    private void assertAuditHeaders(HttpResponse<String> response, String expectedRequestId) {
        assertThat(response.headers().firstValue("X-Request-Id")).contains(expectedRequestId);
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Pragma")).contains("no-cache");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AuditProbeConfiguration {

        @Bean("sqlSessionTemplate1")
        SqlSessionTemplate sqlSessionTemplate1() {
            return mock(SqlSessionTemplate.class);
        }

        @Bean
        AuditLogDao auditLogDao() {
            return mock(AuditLogDao.class);
        }

        @Bean(destroyMethod = "stop")
        Endpoint auditProbeEndpoint(Bus bus) {
            EndpointImpl endpoint = new EndpointImpl(bus, new AuditProbePortImpl());
            endpoint.publish("/soap/audit");
            return endpoint;
        }
    }
}
