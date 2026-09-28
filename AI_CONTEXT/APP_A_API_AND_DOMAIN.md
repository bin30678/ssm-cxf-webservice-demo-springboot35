# app-a API, integration probes, DAO, and domain source

This file is a lossless textual snapshot. Every section contains the complete current content of the source file named in its heading.

## File: app-a/src/main/java/com/example/appa/AppAApplication.java

````java
package com.example.appa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppAApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppAApplication.class, args);
    }
}
````


## File: app-a/src/main/java/com/example/appa/AppAExternalLibAJndiProbe.java

````java
package com.example.appa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppAExternalLibAJndiProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppAExternalLibAJndiProbe.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("External JAR A GenericDao source: {}",
                com.external.liba.utils.GenericDao.class
                        .getProtectionDomain().getCodeSource().getLocation());
        log.info("External JAR A GenericDao JNDI proof: {}", verifyAllConnections());
    }

    public Map<String, String> verifyAllConnections() {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("jdbc/cxfdemo2", verify(
                "jdbc/cxfdemo2", com.external.liba.utils.GenericDao::getConnection1));
        results.put("jdbc/as400_c", verify(
                "jdbc/as400_c", com.external.liba.utils.GenericDao::getConnection2));
        return results;
    }

    private String verify(String expectedName, Supplier<Connection> connectionSupplier) {
        try (Connection connection = connectionSupplier.get()) {
            if (connection == null) {
                throw new IllegalStateException("External JAR JNDI connection is null: " + expectedName);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM connection_probe")) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("External JAR probe query returned no row: " + expectedName);
                }
            }
            return connection.getMetaData().getURL();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to verify external JAR connection " + expectedName, ex);
        }
    }
}
````


## File: app-a/src/main/java/com/example/appa/AppAJndiConnectionProbe.java

````java
package com.example.appa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.example.cxfdemo.utils.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppAJndiConnectionProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppAJndiConnectionProbe.class);

    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> results = verifyAllConnections();
        log.info("Embedded Tomcat JNDI proof: {}", results);
    }

    public Map<String, String> verifyAllConnections() {
        Map<String, String> results = new LinkedHashMap<>();
        results.put("jdbc/cxfdemo1", verify("jdbc/cxfdemo1", GenericDao::getConnection1));
        results.put("jdbc/cxfdemo2", verify("jdbc/cxfdemo2", GenericDao::getConnection2));
        results.put("jdbc/as400_a", verify("jdbc/as400_a", GenericDao::getConnection3));
        results.put("jdbc/as400_b", verify("jdbc/as400_b", GenericDao::getConnection4));
        return results;
    }

    private String verify(String expectedName, Supplier<Connection> connectionSupplier) {
        try (Connection connection = connectionSupplier.get()) {
            if (connection == null) {
                throw new IllegalStateException("JNDI connection is null: " + expectedName);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM connection_probe")) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Probe query returned no row: " + expectedName);
                }
            }
            return connection.getMetaData().getURL();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to verify " + expectedName, ex);
        }
    }
}
````


## File: app-a/src/main/java/com/example/appa/AppAPropertyProbe.java

````java
package com.example.appa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppAPropertyProbe {

    private static final Logger log = LoggerFactory.getLogger(AppAPropertyProbe.class);

    private final String directDatabaseValue;
    private final String valueResolvedInsideApplicationProperties;
    private final String configSource;

    public AppAPropertyProbe(
            @Value("${system.app.message}") String directDatabaseValue,
            @Value("${app-a.application-message}") String valueResolvedInsideApplicationProperties,
            @Value("${app-a.config-source}") String configSource) {
        this.directDatabaseValue = directDatabaseValue;
        this.valueResolvedInsideApplicationProperties = valueResolvedInsideApplicationProperties;
        this.configSource = configSource;
        log.info("DB property proof - @Value: [{}], application placeholder: [{}], source: [{}]",
                directDatabaseValue, valueResolvedInsideApplicationProperties, configSource);
    }

    public String getDirectDatabaseValue() {
        return directDatabaseValue;
    }

    public String getValueResolvedInsideApplicationProperties() {
        return valueResolvedInsideApplicationProperties;
    }

    public String getConfigSource() {
        return configSource;
    }
}
````


## File: app-a/src/main/java/com/example/appa/config/AppACxfConfiguration.java

````java
package com.example.appa.config;

import com.example.cxfdemo.dao.PolicyDaoImpl;
import com.external.liba.dao.ExternalJarGenericDao;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({PolicyDaoImpl.class, ExternalJarGenericDao.class})
// Register legacy JAX-RS annotations as Spring beans, including constructor injection.
@ComponentScan(basePackages = {"com.example.cxfdemo.rest", "com.example.cxfdemo.provider"},
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION,
                classes = {Path.class, Provider.class}))
public class AppACxfConfiguration {

    @Bean
    OpenApiFeature openApiFeature() {
        // OpenApiFeature is a CXF server Feature, so the existing component-scan server attaches it.
        OpenApiFeature feature = new OpenApiFeature();
        feature.setTitle("App A REST API");
        feature.setDescription("JAX-RS endpoints published by app-a");
        feature.setVersion("1.0.0");
        feature.setSupportSwaggerUi(true);
        feature.setSwaggerUiConfig(new SwaggerUiConfig()
                .url("/rest/openapi.json")
                .tryItOutEnabled(true));
        return feature;
    }

    @Bean
    ServletRegistrationBean<CXFServlet> cxfServletRegistration() {
        // One servlet per application, preserving both legacy URL prefixes.
        ServletRegistrationBean<CXFServlet> registration = new ServletRegistrationBean<>(
                new CXFServlet(), "/rest/*", "/Webservice/*");
        registration.setName("CXFServlet");
        registration.setLoadOnStartup(2);
        return registration;
    }
}
````


## File: app-a/src/main/java/com/example/appa/openapi/SwaggerUiInitializerResource.java

````java
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
````


## File: app-a/src/main/java/com/example/cxfdemo/dao/PolicyDao.java

````java
package com.example.cxfdemo.dao;

import com.example.cxfdemo.model.PolicyInfo;

public interface PolicyDao {

    PolicyInfo findPolicyViaMapper(String policyNo);

    PolicyInfo findPolicyViaJdbc(String policyNo);

    PolicyInfo findPolicyViaPureJdbc(String policyNo) throws Exception;

    int insertPolicy(PolicyInfo policy);

    int updatePolicyStatus(String policyNo, String status);
}
````


## File: app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java

````java
package com.example.cxfdemo.dao;

import com.example.cxfdemo.model.PolicyInfo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * SSM business DAO copied from the original application.
 */
@Repository("policyDao")
public class PolicyDaoImpl extends BaseDao implements PolicyDao {

    public PolicyDaoImpl() {
    }

    @Override
    public PolicyInfo findPolicyViaMapper(String policyNo) {
        return sqlSessionTemplate.selectOne("com.example.cxfdemo.mapper.DemoMapper.findPolicy", policyNo);
    }

    @Override
    public PolicyInfo findPolicyViaJdbc(String policyNo) {
        String sql = "SELECT policy_no, holder_name, product_name, status FROM policy_info WHERE policy_no = ?";
        List<PolicyInfo> list = jdbcTemplate.query(sql, new RowMapper<PolicyInfo>() {
            @Override
            public PolicyInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new PolicyInfo(
                        rs.getString("policy_no"),
                        rs.getString("holder_name"),
                        rs.getString("product_name"),
                        rs.getString("status")
                );
            }
        }, policyNo);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public PolicyInfo findPolicyViaPureJdbc(String policyNo) throws Exception {
        Connection conn = DataSourceUtils.getConnection(this.dataSource);
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT policy_no, holder_name, product_name, status FROM policy_info WHERE policy_no = ?");
            ps.setString(1, policyNo);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) {
                    return new PolicyInfo(
                            rs.getString("policy_no"),
                            rs.getString("holder_name"),
                            rs.getString("product_name"),
                            rs.getString("status")
                    );
                }
            } finally {
                rs.close();
                ps.close();
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, this.dataSource);
        }
        return null;
    }

    @Override
    public int insertPolicy(PolicyInfo policy) {
        String sql = "INSERT INTO policy_info (policy_no, holder_name, product_name, status) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, policy.getPolicyNo(), policy.getHolderName(), policy.getProductName(), policy.getStatus());
    }

    @Override
    public int updatePolicyStatus(String policyNo, String status) {
        String sql = "UPDATE policy_info SET status = ? WHERE policy_no = ?";
        return jdbcTemplate.update(sql, status, policyNo);
    }
}
````


## File: app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java

````java
package com.example.cxfdemo.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PolicyInfo", propOrder = {"policyNo", "holderName", "productName", "status"})
public class PolicyInfo {
    private String policyNo;
    private String holderName;
    private String productName;
    private String status;

    public PolicyInfo() {}

    public PolicyInfo(String policyNo, String holderName, String productName, String status) {
        this.policyNo = policyNo;
        this.holderName = holderName;
        this.productName = productName;
        this.status = status;
    }

    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
````


## File: app-a/src/main/java/com/example/cxfdemo/rest/HealthResource.java

````java
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
````


## File: app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java

````java
package com.example.cxfdemo.rest;

import com.example.cxfdemo.dao.PolicyDaoImpl;
import com.example.cxfdemo.fault.ServiceFaultException;
import com.example.cxfdemo.model.PolicyInfo;
import com.external.liba.dao.ExternalJarGenericDao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls one DAO from the main project and one DAO from external-lib-a.
 */
@Path("/mainNouternal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MainNouternalResource {

    private final PolicyDaoImpl policyDao;
    private final ExternalJarGenericDao externalJarGenericDao;

    public MainNouternalResource(PolicyDaoImpl policyDao,
                                 ExternalJarGenericDao externalJarGenericDao) {
        this.policyDao = policyDao;
        this.externalJarGenericDao = externalJarGenericDao;
    }

    @POST
    @Path("/dao")
    public Response executeDaoOperations(DaoOperationRequest request) {
        if (request == null || isBlank(request.getPolicyNo()) || isBlank(request.getCustomerName())) {
            throw new ServiceFaultException(
                    "INVALID_REQUEST", "policyNo and customerName are required", 400);
        }

        String status = isBlank(request.getStatus()) ? "ACTIVE" : request.getStatus();
        PolicyInfo policy = new PolicyInfo(
                request.getPolicyNo(), request.getHolderName(), request.getProductName(), status);

        try {
            int mainRows = policyDao.insertPolicy(policy);
            boolean externalCommitted = externalJarGenericDao.insertCustomerViaInternalLookup(
                    request.getCustomerName(), true);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mainDatabase", "cxfdemo1");
            result.put("mainTable", "policy_info");
            result.put("mainRows", mainRows);
            result.put("policy", policyDao.findPolicyViaJdbc(request.getPolicyNo()));
            result.put("externalDatabase", "cxfdemo2");
            result.put("externalTable", "customers");
            result.put("externalCommitted", externalCommitted);
            result.put("customerName", request.getCustomerName());
            result.put("externalActiveCustomers",
                    externalJarGenericDao.queryActiveCustomersViaInternalLookup());
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception ex) {
            throw new ServiceFaultException("DAO_OPERATION_FAILED", ex.getMessage(), 500);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class DaoOperationRequest {
        private String policyNo;
        private String holderName;
        private String productName;
        private String status;
        private String customerName;

        public DaoOperationRequest() {
        }

        public String getPolicyNo() { return policyNo; }
        public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
        public String getHolderName() { return holderName; }
        public void setHolderName(String holderName) { this.holderName = holderName; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
    }
}
````

