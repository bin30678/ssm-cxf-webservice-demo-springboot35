package com.example.cxfdemo.interceptor;

import com.example.cxfdemo.utils.WebUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;
import com.example.cxfdemo.fault.ServiceFaultException;
import org.apache.cxf.binding.soap.SoapMessage;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.Locale;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class AuditRequestInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditRequestInterceptor.class);
    public static final String AUDIT_GUID = "AUDIT_GUID";

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

    public void setAuditLogDao(com.example.cxfdemo.dao.AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    public AuditRequestInterceptor() {
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        
        String guid = null;
        if (request != null) {
            guid = request.getHeader("X-Request-Id");
            if (guid == null || guid.trim().isEmpty()) {
                guid = request.getHeader("guid");
            }
        }
        if (guid == null || guid.trim().isEmpty()) {
            Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
            if (headers != null) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    if ("X-Request-Id".equalsIgnoreCase(entry.getKey()) || "guid".equalsIgnoreCase(entry.getKey())) {
                        List<String> list = entry.getValue();
                        if (list != null && !list.isEmpty()) {
                            guid = list.get(0);
                            break;
                        }
                    }
                }
            }
        }
        
        if (guid == null || guid.trim().isEmpty()) {
            guid = UUID.randomUUID().toString();
        }
        
        message.getExchange().put(AUDIT_GUID, guid);

        String ip = WebUtils.getClientIp(request);
        String clientType = WebUtils.getClientType(request);
        String hostname = WebUtils.getLocalHostname();
        String uri = (String) message.get(Message.REQUEST_URI);
        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);

        // 讀取並保留 Request Body Payload
        String payload = null;
        try {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                CachedOutputStream cos = new CachedOutputStream();
                IOUtils.copy(is, cos);
                cos.flush();
                byte[] bytes = cos.getBytes();
                if (bytes != null && bytes.length > 0) {
                    payload = new String(bytes, "UTF-8");
                }
                // 重設 InputStream 讓 CXF 框架能繼續讀取
                message.setContent(InputStream.class, cos.getInputStream());
                cos.close();
            }
        } catch (Exception e) {
            log.warn("Failed to capture request payload: {}", e.getMessage());
        }

        if ((payload == null || payload.trim().isEmpty()) && request != null) {
            String qs = request.getQueryString();
            if (qs != null && !qs.trim().isEmpty()) {
                payload = "?" + qs;
            }
        }

        log.info("=== [Audit Request] ===");
        log.info("GUID: {}", guid);
        log.info("Time: {}", new Date());
        log.info("Client IP: {}", ip);
        log.info("Client Type: {}", clientType);
        log.info("Host: {}", hostname);
        log.info("URI: {} {}", method, uri);
        if (payload != null) {
            log.info("Payload: {}", payload.length() > 500 ? payload.substring(0, 500) + "..." : payload);
        }
        log.info("=======================");
        
        if (this.auditLogDao != null) {
            this.auditLogDao.insertRequestLog(guid, ip, clientType, hostname, uri, method, payload);
        } else {
            log.warn("AuditLogDao is not injected or found!");
        }

        // 檢查 Content-Type
        validateContentType(message);
    }

    private static final List<String> BODYLESS_METHODS = Arrays.asList("GET", "HEAD", "OPTIONS");

    private void validateContentType(Message message) throws Fault {
        String method = stringValue(message.get(Message.HTTP_REQUEST_METHOD)).toUpperCase(Locale.ROOT);
        if (BODYLESS_METHODS.contains(method)) {
            return;
        }

        String contentType = baseMediaType(stringValue(message.get(Message.CONTENT_TYPE)));
        boolean soap = message instanceof SoapMessage;
        boolean allowed = soap ? isSoap(contentType)
                : isJson(contentType) || "multipart/form-data".equals(contentType);
        if (!allowed) {
            log.warn("Unsupported Media Type in AuditRequestInterceptor: {}, soap: {}", contentType, soap);
            ServiceFaultException exception = new ServiceFaultException(
                    "UNSUPPORTED_MEDIA_TYPE",
                    soap ? "SOAP 僅接受 text/xml 或 application/soap+xml"
                            : "REST 僅接受 application/json 或 multipart/form-data",
                    415);
            Fault fault = new Fault(exception);
            fault.setStatusCode(415);
            throw fault;
        }
    }

    private String baseMediaType(String contentType) {
        int separator = contentType.indexOf(';');
        return (separator < 0 ? contentType : contentType.substring(0, separator))
                .trim().toLowerCase(Locale.ROOT);
    }

    private boolean isJson(String contentType) {
        return "application/json".equals(contentType)
                || contentType.startsWith("application/") && contentType.endsWith("+json");
    }

    private boolean isSoap(String contentType) {
        return "text/xml".equals(contentType) || "application/soap+xml".equals(contentType);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
