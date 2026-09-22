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
