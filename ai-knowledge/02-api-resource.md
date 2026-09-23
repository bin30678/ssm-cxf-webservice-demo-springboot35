# API 端點與 Web 服務資源 (API Resources & Web Services)

本文件收錄專案中所有 JAX-RS RESTful API 資源類別、JAX-WS WebService 服務端點介面與實作。所有類別方法與註解均完整無損封裝。

> 本文件收錄 5 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: app-a/src/main/java/com/example/cxfdemo/rest/HealthResource.java

```java
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

```

---

## File: app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java

```java
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

```

---

## File: app-a/src/test/java/com/example/appa/AuditProbePort.java

```java
package com.example.appa;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService(name = "AuditProbePort", targetNamespace = "https://example.com/app-a/audit")
public interface AuditProbePort {

    @WebMethod
    @WebResult(name = "result")
    String ping(@WebParam(name = "value") String value);
}

```

---

## File: app-a/src/test/java/com/example/appa/AuditProbePortImpl.java

```java
package com.example.appa;

import com.example.cxfdemo.fault.ServiceFaultException;
import jakarta.jws.WebService;

@WebService(
        endpointInterface = "com.example.appa.AuditProbePort",
        serviceName = "AuditProbeService",
        portName = "AuditProbePort",
        targetNamespace = "https://example.com/app-a/audit")
public class AuditProbePortImpl implements AuditProbePort {

    @Override
    public String ping(String value) {
        if ("FAIL".equals(value)) {
            throw new ServiceFaultException("PROBE_ERROR", "SOAP 測試錯誤", 422);
        }
        return "PONG:" + value;
    }
}

```

---

## File: app-a/src/test/java/com/example/cxfdemo/rest/ScannedProbeResource.java

```java
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

```

---
