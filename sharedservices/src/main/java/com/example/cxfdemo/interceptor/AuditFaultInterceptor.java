package com.example.cxfdemo.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AuditFaultInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditFaultInterceptor.class);

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

    public void setAuditLogDao(com.example.cxfdemo.dao.AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    public AuditFaultInterceptor() {
        super(Phase.MARSHAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String guid = null;
        if (message.getExchange() != null) {
            guid = (String) message.getExchange().get(AuditRequestInterceptor.AUDIT_GUID);
        }
        
        Exception ex = message.getContent(Exception.class);
        String errorMsg = ex != null ? ex.getMessage() : "Unknown Fault";

        log.error("=== [Audit Fault] ===");
        log.error("GUID: {}", guid);
        log.error("Time: {}", new Date());
        log.error("Error: {}", errorMsg);
        log.error("=====================");
        
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (headers == null) {
            headers = new HashMap<>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }

        addHeaderIfNotExists(headers, "X-Content-Type-Options", "nosniff");
        addHeaderIfNotExists(headers, "X-XSS-Protection", "1; mode=block");
        addHeaderIfNotExists(headers, "Cache-Control", "no-store");
        addHeaderIfNotExists(headers, "Pragma", "no-cache");

        if (guid != null) {
            addHeaderIfNotExists(headers, "X-Request-Id", guid);
        }

        if (this.auditLogDao != null) {
            this.auditLogDao.insertFaultLog(guid, errorMsg);
        } else {
            log.warn("AuditLogDao is not injected or found!");
        }
    }

    private void addHeaderIfNotExists(Map<String, List<String>> headers, String name, String value) {
        if (!headers.containsKey(name)) {
            headers.put(name, new ArrayList<>(Arrays.asList(value)));
        }
    }
}
