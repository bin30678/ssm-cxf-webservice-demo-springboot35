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
