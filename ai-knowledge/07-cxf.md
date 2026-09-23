# Apache CXF 攔截器、例外與提供者 (CXF Interceptors, Faults & Providers)

本文件包含 Apache CXF 請求/回應/錯誤階段攔截器 (AuditRequestInterceptor, AuditResponseInterceptor, AuditFaultInterceptor, UnifiedFaultInterceptor)、自訂 JAX-RS Provider (GsonProvider) 以及 SOAP Fault/例外模型。

> 本文件收錄 7 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: sharedservices/src/main/java/com/example/cxfdemo/fault/ApiError.java

```java
package com.example.cxfdemo.fault;

import java.time.OffsetDateTime;

public class ApiError {
    private final boolean success = false;
    private final String code;
    private final String message;
    private final String traceId;
    private final String timestamp;

    public ApiError(String code, String message, String traceId) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = OffsetDateTime.now().toString();
    }

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getTraceId() { return traceId; }
    public String getTimestamp() { return timestamp; }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java

```java
package com.example.cxfdemo.fault;

public class ServiceFaultException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public ServiceFaultException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public int getHttpStatus() { return httpStatus; }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditFaultInterceptor.java

```java
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

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java

```java
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

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditResponseInterceptor.java

```java
package com.example.cxfdemo.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AuditResponseInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditResponseInterceptor.class);

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

    public void setAuditLogDao(com.example.cxfdemo.dao.AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    public com.example.cxfdemo.dao.AuditLogDao getAuditLogDao() {
        return this.auditLogDao;
    }

    public AuditResponseInterceptor() {
        // PRE_STREAM 階段才能攔截底層的 OutputStream
        super(Phase.PRE_STREAM);
        // 確保在寫出前執行
        addBefore(StaxOutInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (headers == null) {
            headers = new HashMap<>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }

        addHeaderIfNotExists(headers, "X-Content-Type-Options", "nosniff");
        addHeaderIfNotExists(headers, "X-XSS-Protection", "1; mode=block");
        addHeaderIfNotExists(headers, "Cache-Control", "no-store");
        addHeaderIfNotExists(headers, "Pragma", "no-cache");

        if (message.getExchange() != null) {
            Object traceId = message.getExchange().get(AuditRequestInterceptor.AUDIT_GUID);
            if (traceId != null) {
                addHeaderIfNotExists(headers, "X-Request-Id", traceId.toString());
            }
        }

        OutputStream os = message.getContent(OutputStream.class);
        if (os == null) {
            return;
        }

        // 將原始 OutputStream 包裝起來，這樣能將資料寫入記憶體緩衝區
        CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
        message.setContent(OutputStream.class, newOut);

        // 註冊回呼 (Callback)，當 Stream onClose() 時就會觸發
        newOut.registerCallback(new LoggingCallback(message, os, this));
    }

    private void addHeaderIfNotExists(Map<String, List<String>> headers, String name, String value) {
        if (!headers.containsKey(name)) {
            headers.put(name, new ArrayList<>(Arrays.asList(value)));
        }
    }

    private static class LoggingCallback implements CachedOutputStreamCallback {
        private final Message message;
        private final OutputStream origStream;
        private final AuditResponseInterceptor parent;
        private final java.util.concurrent.atomic.AtomicBoolean hasLogged = new java.util.concurrent.atomic.AtomicBoolean(false);

        public LoggingCallback(Message message, OutputStream origStream, AuditResponseInterceptor parent) {
            this.message = message;
            this.origStream = origStream;
            this.parent = parent;
        }

        @Override
        public void onFlush(CachedOutputStream cos) {
            // Do nothing
        }

        @Override
        public void onClose(CachedOutputStream cos) {
            if (!hasLogged.compareAndSet(false, true)) {
                return;
            }
            try {
                String guid = null;
                if (message.getExchange() != null) {
                    guid = (String) message.getExchange().get(AuditRequestInterceptor.AUDIT_GUID);
                }
                
                Integer responseCode = (Integer) message.get(Message.RESPONSE_CODE);
                if (responseCode == null) {
                    responseCode = 200; // 預設成功代碼
                }

                // 從緩衝區中讀取出實際寫入的 Payload
                StringBuilder payload = new StringBuilder();
                try (InputStream is = cos.getInputStream()) {
                    byte[] bytes = new byte[is.available()];
                    is.read(bytes);
                    payload.append(new String(bytes, "UTF-8"));
                } catch (Exception e) {
                    payload.append("Error reading payload: ").append(e.getMessage());
                }

                String payloadStr = payload.toString();
                log.info("=== [Audit Response (onClose)] ===");
                log.info("GUID: {}", guid);
                log.info("Time: {}", new Date());
                log.info("Response Code: {}", responseCode);
                log.info("Payload: {}", payloadStr.length() > 500 ? payloadStr.substring(0, 500) + "..." : payloadStr);
                log.info("==================================");
                
                com.example.cxfdemo.dao.AuditLogDao dao = parent != null ? parent.getAuditLogDao() : null;
                if (dao != null) {
                    dao.insertResponseLog(guid, responseCode, payloadStr);
                } else {
                    log.warn("AuditLogDao is not injected or found!");
                }

            } catch (Exception e) {
                log.error("AuditResponseInterceptor callback error", e);
            }
        }
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/interceptor/UnifiedFaultInterceptor.java

```java
package com.example.cxfdemo.interceptor;

import javax.xml.namespace.QName;

import com.example.cxfdemo.fault.ServiceFaultException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class UnifiedFaultInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(UnifiedFaultInterceptor.class);

    private static final String ERROR_NAMESPACE = "https://example.com/cxfdemo/error";

    public UnifiedFaultInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addBefore("org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor");
        addBefore("org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor");
    }

    @Override
    public void handleMessage(Message message) {
        if (!(message instanceof SoapMessage)) {
            return;
        }
        Exception original = message.getContent(Exception.class);
        ServiceFaultException serviceFault = findCause(original, ServiceFaultException.class);
        String code = serviceFault == null ? "INTERNAL_ERROR" : serviceFault.getCode();
        String safeMessage = serviceFault == null ? "服務處理失敗" : serviceFault.getMessage();
        int status = serviceFault == null ? 500 : serviceFault.getHttpStatus();
        QName faultCode = status >= 500 ? Fault.FAULT_CODE_SERVER : Fault.FAULT_CODE_CLIENT;

        log.warn("UnifiedFaultInterceptor.handleMessage transforming exception, code: {}, status: {}, message: {}", code, status, safeMessage);

        SoapFault soapFault = new SoapFault(safeMessage, original, faultCode);
        soapFault.setStatusCode(status);
        Element detail = soapFault.getOrCreateDetail();
        Document document = detail.getOwnerDocument();
        Element serviceError = document.createElementNS(ERROR_NAMESPACE, "err:ServiceError");
        append(document, serviceError, "code", code);
        append(document, serviceError, "message", safeMessage);
        Object traceValue = message.getExchange() == null ? null
                : message.getExchange().get(AuditRequestInterceptor.AUDIT_GUID);
        String traceId = traceValue == null ? null : traceValue.toString();
        append(document, serviceError, "traceId", traceId == null ? "" : traceId);
        detail.appendChild(serviceError);
        message.setContent(Exception.class, soapFault);
    }

    private void append(Document document, Element parent, String name, String value) {
        Element child = document.createElementNS(ERROR_NAMESPACE, "err:" + name);
        child.setTextContent(value);
        parent.appendChild(child);
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/provider/GsonProvider.java

```java
package com.example.cxfdemo.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GsonProvider<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

    private final Gson gson;

    public GsonProvider() {
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try (Reader reader = new InputStreamReader(entityStream, "UTF-8")) {
            return gson.fromJson(reader, genericType);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try (Writer writer = new OutputStreamWriter(entityStream, "UTF-8")) {
            gson.toJson(t, genericType, writer);
        }
    }
}

```

---
