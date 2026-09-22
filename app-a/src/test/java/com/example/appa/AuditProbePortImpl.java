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
