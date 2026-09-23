# 整合測試與單元測試套件 (Integration & Unit Tests)

本文件收錄針對資料庫存取、CXF 攔截器鏈、系統屬性啟動環境、JAX-RS 資源以及定時排程的整合與單元測試類別。

> 本文件收錄 6 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: app-a/src/test/java/com/example/appa/BaseDaoMainDatabaseTest.java

```java
package com.example.appa;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.dao.BaseDao;
import com.example.sharedservices.database.MainDatabaseAutoConfiguration;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class BaseDaoMainDatabaseTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MainDatabaseAutoConfiguration.class))
            .withUserConfiguration(Databases.class)
            .withBean(ConsumerDao.class);

    @Test
    void independentConsumersUseMainDatabaseEvenWithSecondaryPrimaryBeans() {
        // Two contexts represent two consumers of the shared JAR, not a shared Spring container.
        runner.run(first -> {
            assertThat(first).hasNotFailed();
            verifyMainDatabase(first.getBean(ConsumerDao.class));
            runner.run(second -> {
                assertThat(second).hasNotFailed();
                ConsumerDao secondDao = second.getBean(ConsumerDao.class);
                verifyMainDatabase(secondDao);
                assertThat(secondDao.getDataSource())
                        .isNotSameAs(first.getBean(ConsumerDao.class).getDataSource());
            });
        });
    }

    @Test
    void missingMainDatabaseFailsInsteadOfFallingBackToSecondary() {
        new ApplicationContextRunner()
                .withUserConfiguration(SecondaryDatabase.class)
                .withBean(ConsumerDao.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("sqlSessionTemplate1");
                });
    }

    private void verifyMainDatabase(ConsumerDao dao) throws Exception {
        // Reads exercise all three inherited access paths against distinguishable databases.
        assertThat(dao.getJdbcTemplate().queryForObject("SELECT label FROM db_identity", String.class))
                .isEqualTo("MAIN");
        var configuration = dao.getSqlSessionTemplate().getConfiguration();
        if (!configuration.hasMapper(IdentityMapper.class)) {
            configuration.addMapper(IdentityMapper.class);
        }
        assertThat(dao.getSqlSessionTemplate().getMapper(IdentityMapper.class).read()).isEqualTo("MAIN");
        try (Connection connection = dao.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT label FROM db_identity")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("MAIN");
        }
    }

    public interface IdentityMapper {
        @Select("SELECT label FROM db_identity")
        String read();
    }

    static class ConsumerDao extends BaseDao { }

    @TestConfiguration(proxyBeanMethods = false)
    static class Databases extends SecondaryDatabase {
        @Bean("dataSource1")
        DataSource mainDataSource() {
            return database("main", "MAIN");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecondaryDatabase {
        @Bean("dataSource2")
        @Primary
        DataSource secondaryDataSource() {
            return database("secondary", "SECONDARY");
        }

        @Bean("jdbcTemplate2")
        @Primary
        JdbcTemplate secondaryJdbcTemplate(@Qualifier("dataSource2") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean("sqlSessionTemplate2")
        @Primary
        SqlSessionTemplate secondarySqlSessionTemplate(@Qualifier("dataSource2") DataSource dataSource)
                throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            SqlSessionFactory sessionFactory = factory.getObject();
            return new SqlSessionTemplate(sessionFactory);
        }
    }

    private static DataSource database(String name, String label) {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:base_dao_" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS db_identity (id INT PRIMARY KEY, label VARCHAR(20))");
        jdbc.update("MERGE INTO db_identity (id, label) KEY(id) VALUES (1, ?)", label);
        return dataSource;
    }
}

```

---

## File: app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java

```java
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

```

---

## File: app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java

```java
package com.example.appa;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.utils.GenericDao;
import com.example.cxfdemo.dao.ExternalApiLogDao;
import com.example.cxfdemo.service.ApiService;
import com.example.cxfdemo.service.MailService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.net.URL;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

class DatabaseSystemPropertiesStartupTest {

    @Test
    void databaseValuesResolveApplicationPlaceholdersAndValueInjectionAtStartup() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AppAApplication.class)
                .web(WebApplicationType.SERVLET)
                .run("--spring.main.banner-mode=off")) {
            AppAPropertyProbe probe = context.getBean(AppAPropertyProbe.class);

            assertThat(context.getBean(ApiService.class)).isNotNull();
            assertThat(context.getBean(MailService.class)).isNotNull();
            assertThat(context.getBean(ExternalApiLogDao.class)).isNotNull();
            var servletContext = ((WebApplicationContext) context).getServletContext();
            assertThat(servletContext.getAttribute("hasTiffReader")).isInstanceOf(Boolean.class);
            assertThat(servletContext.getAttribute("targetFont")).isEqualTo("\u6a19\u6977\u9ad4");
            assertThat(servletContext.getAttribute("fontExists")).isInstanceOf(Boolean.class);

            assertThat(context.getEnvironment().getProperty("system.app.message"))
                    .isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(context.getEnvironment().getProperty("app-a.application-message"))
                    .isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(probe.getDirectDatabaseValue()).isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(probe.getValueResolvedInsideApplicationProperties()).isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            assertThat(probe.getConfigSource()).isEqualTo("system_properties");
            assertThat(context.getBean(AppAJndiConnectionProbe.class).verifyAllConnections())
                    .containsOnlyKeys("jdbc/cxfdemo1", "jdbc/cxfdemo2", "jdbc/as400_a", "jdbc/as400_b")
                    .allSatisfy((name, url) -> assertThat(url).startsWith("jdbc:h2:mem:"));
            assertThat(context.getBean(AppAExternalLibAJndiProbe.class).verifyAllConnections())
                    .containsEntry("jdbc/cxfdemo2", "jdbc:h2:mem:cxfdemo2")
                    .containsEntry("jdbc/as400_c", "jdbc:h2:mem:as400_c");

            URL externalJarLocation = com.external.liba.utils.GenericDao.class
                    .getProtectionDomain().getCodeSource().getLocation();
            assertThat(externalJarLocation.getPath()).endsWith("external-lib-a-1.0.0.jar");
            assertThat(GenericDao.getSystemProperty("system.app.message"))
                    .isEqualTo("DB_STARTUP_PROPERTY_LOADED");

            DataSource bootDataSource = context.getBean(DataSource.class);
            try (Connection connection = bootDataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT prop_value FROM system_properties WHERE prop_key = 'system.app.message'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo("DB_STARTUP_PROPERTY_LOADED");
            }
        }
    }
}

```

---

## File: app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java

```java
package com.example.appa;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.utils.GenericDao;
import com.example.cxfdemo.dao.PolicyDaoImpl;
import com.example.cxfdemo.provider.GsonProvider;
import com.example.cxfdemo.rest.HealthResource;
import javax.sql.DataSource;
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
            assertThat(context.getBeansOfType(HealthResource.class)).hasSize(1);
            assertThat(context.getBeansOfType(GsonProvider.class)).hasSize(1);
            PolicyDaoImpl policyDao = context.getBean(PolicyDaoImpl.class);
            DataSource mainDataSource = context.getBean("dataSource1", DataSource.class);
            assertThat(policyDao.getDataSource()).isSameAs(mainDataSource);
            assertThat(policyDao.getJdbcTemplate().getDataSource()).isSameAs(mainDataSource);
            assertThat(policyDao.getSqlSessionTemplate().getSqlSessionFactory()
                    .getConfiguration().getEnvironment().getDataSource()).isSameAs(mainDataSource);
            try (Connection connection = policyDao.getDataSource().getConnection()) {
                assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:cxfdemo1");
            }
            HttpResponse<String> scanResponse = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/rest/scan-proof"))
                            .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(scanResponse.statusCode()).isEqualTo(200);
            assertThat(scanResponse.body()).contains("DB_STARTUP_PROPERTY_LOADED");

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

```

---

## File: app-b/src/test/java/com/example/appb/AppBSchedulingIntegrationTest.java

```java
package com.example.appb;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.scheduler.ScheduledTasks;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTaskHolder;

class AppBSchedulingIntegrationTest {

    @Test
    void appBRegistersLegacyScheduledAndQuartzJobs() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AppBApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.main.banner-mode=off", "--spring.quartz.auto-startup=false")) {
            assertThat(context.getBean(ScheduledTasks.class)).isNotNull();

            ScheduledTaskHolder scheduledTaskHolder = context.getBean(ScheduledTaskHolder.class);
            assertThat(scheduledTaskHolder.getScheduledTasks()).hasSize(4);

            Set<String> cronExpressions = Arrays.stream(ScheduledTasks.class.getDeclaredMethods())
                    .map(method -> method.getAnnotation(Scheduled.class))
                    .filter(annotation -> annotation != null)
                    .map(Scheduled::cron)
                    .collect(Collectors.toSet());
            assertThat(cronExpressions).containsExactlyInAnyOrder(
                    "0 0 * * * *", "0 0 0 * * *", "0 0 1 * * *", "0 0 12 * * *");

            JobDetail jobDetail = context.getBean("quartzDemoJobDetail", JobDetail.class);
            Trigger trigger = context.getBean("quartzDemoTrigger", Trigger.class);
            assertThat(jobDetail.getJobClass().getName())
                    .isEqualTo("com.example.cxfdemo.scheduler.QuartzDemoJob");
            assertThat(jobDetail.isDurable()).isTrue();
            assertThat(trigger.getJobKey()).isEqualTo(jobDetail.getKey());
            assertThat(trigger).isInstanceOf(SimpleTrigger.class);
            assertThat(((SimpleTrigger) trigger).getRepeatInterval()).isEqualTo(300000L);
        }
    }
}

```

---

## File: app-b/src/test/java/com/example/appb/ScheduledTasksTest.java

```java
package com.example.appb;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cxfdemo.scheduler.ScheduledTasks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduledTasksTest {

    @TempDir
    Path testDirectory;

    @Test
    void cleanOldBackupFilesDeletesExpiredFilesRecursively() throws Exception {
        Path newFile = Files.createFile(testDirectory.resolve("new_backup.txt"));
        Path oldFile = Files.createFile(testDirectory.resolve("old_backup.txt"));
        Path subDirectory = Files.createDirectory(testDirectory.resolve("sub_folder"));
        Path oldFileInSubDirectory = Files.createFile(subDirectory.resolve("old_sub_backup.txt"));
        FileTime tenDaysAgo = FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Files.setLastModifiedTime(oldFile, tenDaysAgo);
        Files.setLastModifiedTime(oldFileInSubDirectory, tenDaysAgo);

        Map<String, Object> result = new ScheduledTasks()
                .cleanOldBackupFiles(testDirectory.toString(), 7);

        assertThat(result).containsEntry("success", true)
                .containsEntry("deletedFilesCount", 2)
                .containsEntry("deletedDirectoriesCount", 1);
        assertThat(newFile).exists();
        assertThat(oldFile).doesNotExist();
        assertThat(oldFileInSubDirectory).doesNotExist();
        assertThat(subDirectory).doesNotExist();
    }
}

```

---
