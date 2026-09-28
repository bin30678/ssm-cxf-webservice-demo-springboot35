# Shared database, JNDI, service, and utility source

This file is a lossless textual snapshot. Every section contains the complete current content of the source file named in its heading.

## File: sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java

````java
package com.example.cxfdemo.config;

import com.example.cxfdemo.utils.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系統參數快取單例類別 (ConfigSingleton)
 * 負責從 system_properties 資料庫表載入系統設定資訊。
 */
public class ConfigSingleton {

    private static final Logger log = LoggerFactory.getLogger(ConfigSingleton.class);

    private static volatile ConfigSingleton instance;

    private final Map<String, String> map = new ConcurrentHashMap<String, String>();

    private ConfigSingleton() {
    }

    /**
     * 取得單例實例，若需初始化 (needInit為true) 則自動執行 load()
     */
    public static ConfigSingleton getInstance() {
        if (instance == null) {
            synchronized (ConfigSingleton.class) {
                if (instance == null) {
                    instance = new ConfigSingleton();
                }
            }
        }
        if (instance.needInit()) {
            synchronized (instance) {
                if (instance.needInit()) {
                    instance.load();
                }
            }
        }
        return instance;
    }

    /**
     * 判斷 map 是否為空 (需要初始化)
     */
    public boolean needInit() {
        return map == null || map.isEmpty();
    }

    /**
     * 從 system_properties 讀取所有資料放進 map 裡
     */
    public synchronized void load() {
        log.info("ConfigSingleton.load() starting...");
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = GenericDao.getConnection1();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT prop_key, prop_value FROM system_properties");
                rs = ps.executeQuery();
                while (rs.next()) {
                    String key = rs.getString("prop_key");
                    String val = rs.getString("prop_value");
                    if (key != null) {
                        map.put(key, val != null ? val : "");
                        log.debug("Loaded ConfigSingleton property: {} = {}", key, val);
                    }
                }
                log.info("ConfigSingleton.load() completed, loaded {} properties.", map.size());
            } else {
                log.warn("ConfigSingleton.load() failed to obtain connection from GenericDao.getConnection1()");
            }
        } catch (Exception e) {
            log.error("Error loading properties in ConfigSingleton", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            GenericDao.closeConnection(conn);
        }
    }

    /**
     * 清除 map
     */
    public synchronized void clear() {
        if (map != null) {
            map.clear();
            log.info("ConfigSingleton.clear() executed.");
        }
    }

    /**
     * 調用 clear，再調用 load
     */
    public synchronized void reload() {
        log.info("ConfigSingleton.reload() triggered.");
        clear();
        load();
    }

    /**
     * 取得 map 內部資料
     */
    public Map<String, String> getMap() {
        return map;
    }

    /**
     * 靜態便捷存取方法：傳 key 進去，取得資訊
     */
    public static String getMappingData(String key) {
        if (key == null) {
            return null;
        }
        return getInstance().getMap().get(key);
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java

````java
package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class AuditLogDao {

    private static final Logger log = LoggerFactory.getLogger(AuditLogDao.class);

    @Resource(name = "sqlSessionTemplate1")
    private SqlSessionTemplate sqlSessionTemplate;

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void insertRequestLog(String guid, String ip, String clientType, String hostname, String uri, String method) {
        insertRequestLog(guid, ip, clientType, hostname, uri, method, null);
    }

    public void insertRequestLog(String guid, String ip, String clientType, String hostname, String uri, String method, String payload) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("ip", ip);
        param.put("clientType", clientType);
        param.put("hostname", hostname);
        param.put("uri", uri);
        param.put("method", method);
        param.put("payload", payload);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertRequestLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertRequestLog for guid: " + guid, e);
        }
    }

    public void insertResponseLog(String guid, Integer responseCode) {
        insertResponseLog(guid, responseCode, null);
    }

    public void insertResponseLog(String guid, Integer responseCode, String payload) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("responseCode", responseCode);
        param.put("payload", payload);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertResponseLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertResponseLog for guid: " + guid, e);
        }
    }

    public void insertFaultLog(String guid, String errorMsg) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("errorMsg", errorMsg);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertFaultLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertFaultLog for guid: " + guid, e);
        }
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java

````java
package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 主專案 BaseDao 抽象類別 (SSM 基準實作)。
 * 持有 SqlSessionTemplate、JdbcTemplate、DataSource 三個欄位。
 * 採用屬性注入（Field Injection / Setter Injection），由 Spring 容器自動注入主庫 (cxfdemo1) 相關元件。
 */
public abstract class BaseDao {

    @Autowired
    @Qualifier("sqlSessionTemplate1")
    protected SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    @Qualifier("jdbcTemplate1")
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("dataSource1")
    protected DataSource dataSource;

    public BaseDao() {
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java

````java
package com.example.cxfdemo.dao;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ExternalApiLogDao extends BaseDao {

    public void insertApiLog(String url, String requestParam, String responseBody, String exceptionMsg, Date callTime) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("url", url);
        param.put("requestParam", requestParam);
        param.put("responseBody", responseBody);
        param.put("exceptionMsg", exceptionMsg);
        param.put("callTime", callTime);
        
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("ExternalApiLog.insertApiLog", param);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/model/Mail.java

````java
package com.example.cxfdemo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 郵件資料模型 (Mail DTO)
 * 包含主旨、內文、收件者、副本與附件資訊。
 */
public class Mail implements Serializable {

    private static final long serialVersionUID = 1L;

    private String subject;
    private String content;
    private List<String> to = new ArrayList<String>();
    private List<String> cc = new ArrayList<String>();
    private List<String> attachmentPaths = new ArrayList<String>();

    public Mail() {
    }

    public Mail(String subject, String content) {
        this.subject = subject;
        this.content = content;
    }

    public Mail(String subject, String content, List<String> to) {
        this.subject = subject;
        this.content = content;
        if (to != null) {
            this.to = to;
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public void addTo(String recipient) {
        if (recipient != null && !recipient.trim().isEmpty()) {
            this.to.add(recipient);
        }
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public void addCc(String ccRecipient) {
        if (ccRecipient != null && !ccRecipient.trim().isEmpty()) {
            this.cc.add(ccRecipient);
        }
    }

    public List<String> getAttachmentPaths() {
        return attachmentPaths;
    }

    public void setAttachmentPaths(List<String> attachmentPaths) {
        this.attachmentPaths = attachmentPaths;
    }

    public void addAttachmentPath(String path) {
        if (path != null && !path.trim().isEmpty()) {
            this.attachmentPaths.add(path);
        }
    }

    @Override
    public String toString() {
        return "Mail{" +
                "subject='" + subject + '\'' +
                ", to=" + to +
                ", cc=" + cc +
                ", attachmentPaths=" + attachmentPaths +
                '}';
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/service/ApiService.java

````java
package com.example.cxfdemo.service;

import java.util.Map;

/**
 * 外部 API 調用服務介面 (ApiService)
 */
public interface ApiService {

    /**
     * 透過 URL Key 從 ConfigSingleton 取得 URL，以 GET 方法呼叫外部 API
     *
     * @param urlKey ConfigSingleton 中的 URL key (例如: "external.api.url")
     * @param queryParams 查詢參數 (Key-Value)
     * @return 外部 API 回應內容 (String)
     */
    String get(String urlKey, Map<String, String> queryParams);

    /**
     * 透過 URL Key 從 ConfigSingleton 取得 URL，以 POST 方法呼叫外部 API
     *
     * @param urlKey ConfigSingleton 中的 URL key (例如: "external.api.url")
     * @param requestBody 請求內文 (例如 JSON 或 XML 字串)
     * @return 外部 API 回應內容 (String)
     */
    String post(String urlKey, String requestBody);

    /**
     * 透過 URL Key 從 ConfigSingleton 取得 URL，以 POST 方法呼叫外部 API (含自訂標頭)
     *
     * @param urlKey ConfigSingleton 中的 URL key
     * @param requestBody 請求內文
     * @param headers 請求標頭
     * @return 外部 API 回應內容 (String)
     */
    String post(String urlKey, String requestBody, Map<String, String> headers);
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java

````java
package com.example.cxfdemo.service;

import com.example.cxfdemo.config.ConfigSingleton;
import com.example.cxfdemo.dao.ExternalApiLogDao;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * 外部 API 調用服務實作類別 (ApiServiceImpl)
 * 透過 ConfigSingleton 讀取 URL，使用 HttpClient 發送請求，
 * 判斷回應碼 200 為成功，非 200 或例外時拋出 RuntimeException，
 * 並在 finally 區塊將請求歷程與結果記錄至 external_api_log。
 */
@Service("apiService")
public class ApiServiceImpl implements ApiService {

    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);

    @Resource
    private ExternalApiLogDao externalApiLogDao;

    public void setExternalApiLogDao(ExternalApiLogDao externalApiLogDao) {
        this.externalApiLogDao = externalApiLogDao;
    }

    /**
     * 提供測試或擴充使用之 HttpClient 建立方法
     */
    protected CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }

    @Override
    public String get(String urlKey, Map<String, String> queryParams) {
        String url = getUrlByKey(urlKey);
        String finalUrl = url;
        String requestParamStr = queryParams != null ? queryParams.toString() : null;
        String responseBody = null;
        String exceptionMsg = null;
        Date callTime = new Date();

        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            if (queryParams != null && !queryParams.isEmpty()) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    uriBuilder.addParameter(entry.getKey(), entry.getValue());
                }
            }
            URI uri = uriBuilder.build();
            finalUrl = uri.toString();

            log.info("ApiServiceImpl.get calling urlKey: {}, URL: {}", urlKey, finalUrl);
            HttpGet httpGet = new HttpGet(uri);

            try (CloseableHttpClient httpClient = createHttpClient();
                 CloseableHttpResponse response = httpClient.execute(httpGet)) {

                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8.name());
                }

                if (statusCode != 200) {
                    exceptionMsg = "External API returned non-200 HTTP status: " + statusCode + ", body: " + responseBody;
                    log.error("ApiServiceImpl.get failed: {}", exceptionMsg);
                    throw new RuntimeException(exceptionMsg);
                }

                log.info("ApiServiceImpl.get success for URL: {}", finalUrl);
                return responseBody;
            }
        } catch (Exception e) {
            if (exceptionMsg == null) {
                exceptionMsg = e.getMessage();
            }
            log.error("ApiServiceImpl.get error for urlKey: {}, URL: {}", urlKey, finalUrl, e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("External API GET call failed: " + e.getMessage(), e);
        } finally {
            recordLog(finalUrl, requestParamStr, responseBody, exceptionMsg, callTime);
        }
    }

    @Override
    public String post(String urlKey, String requestBody) {
        return post(urlKey, requestBody, null);
    }

    @Override
    public String post(String urlKey, String requestBody, Map<String, String> headers) {
        String url = getUrlByKey(urlKey);
        String responseBody = null;
        String exceptionMsg = null;
        Date callTime = new Date();

        try {
            log.info("ApiServiceImpl.post calling urlKey: {}, URL: {}", urlKey, url);
            HttpPost httpPost = new HttpPost(url);

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.addHeader(entry.getKey(), entry.getValue());
                }
            }
            // 若未指定 Content-Type 且含有內文，預設使用 application/json
            if (httpPost.getFirstHeader("Content-Type") == null && requestBody != null) {
                httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
            }

            if (requestBody != null) {
                httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8.name()));
            }

            try (CloseableHttpClient httpClient = createHttpClient();
                 CloseableHttpResponse response = httpClient.execute(httpPost)) {

                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8.name());
                }

                if (statusCode != 200) {
                    exceptionMsg = "External API returned non-200 HTTP status: " + statusCode + ", body: " + responseBody;
                    log.error("ApiServiceImpl.post failed: {}", exceptionMsg);
                    throw new RuntimeException(exceptionMsg);
                }

                log.info("ApiServiceImpl.post success for URL: {}", url);
                return responseBody;
            }
        } catch (Exception e) {
            if (exceptionMsg == null) {
                exceptionMsg = e.getMessage();
            }
            log.error("ApiServiceImpl.post error for urlKey: {}, URL: {}", urlKey, url, e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("External API POST call failed: " + e.getMessage(), e);
        } finally {
            recordLog(url, requestBody, responseBody, exceptionMsg, callTime);
        }
    }

    /**
     * 從 ConfigSingleton 取得 URL，若找不到則拋出異常
     */
    private String getUrlByKey(String urlKey) {
        if (urlKey == null || urlKey.trim().isEmpty()) {
            throw new IllegalArgumentException("urlKey must not be null or empty");
        }
        String url = ConfigSingleton.getMappingData(urlKey);
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("URL not configured in ConfigSingleton for key: " + urlKey);
        }
        return url.trim();
    }

    /**
     * 記錄到 external_api_log
     */
    private void recordLog(String url, String param, String responseBody, String exceptionMsg, Date callTime) {
        try {
            if (externalApiLogDao != null) {
                externalApiLogDao.insertApiLog(url, param, responseBody, exceptionMsg, callTime);
            } else {
                log.warn("ExternalApiLogDao is not injected, skipping log insert.");
            }
        } catch (Exception e) {
            log.error("Failed to insert external_api_log", e);
        }
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java

````java
package com.example.cxfdemo.service;

import com.example.cxfdemo.model.Mail;

/**
 * 郵件發送服務介面 (MailService)
 */
public interface MailService {

    /**
     * 依據 Mail 傳入之主旨、內文、收件者與附件發送郵件。
     * 寄件者 (from) 將動態透過 ConfigSingleton.getMappingData() 讀取。
     *
     * @param mail 郵件模型 DTO
     */
    void sendMail(Mail mail);
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java

````java
package com.example.cxfdemo.service;

import com.example.cxfdemo.config.ConfigSingleton;
import com.example.cxfdemo.model.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.Properties;

/**
 * 郵件發送服務實作類別 (MailServiceImpl)
 * Mail 相關連線設定 (Host, Port, Account, Auth) 直接於 Impl 內部程式碼手動建立，自 ConfigSingleton 讀取，不透過 Spring 設定檔。
 */
@Service("mailService")
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    /**
     * 於 Impl 內部直接建立與配置 JavaMailSenderImpl
     */
    private JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 從 ConfigSingleton 讀取連線屬性
        String host = ConfigSingleton.getMappingData("mail.host");
        if (host == null || host.trim().isEmpty()) {
            host = ConfigSingleton.getMappingData("mail.smtp.host");
        }
        if (host == null || host.trim().isEmpty()) {
            host = "smtp.example.com";
        }

        String portStr = ConfigSingleton.getMappingData("mail.port");
        if (portStr == null || portStr.trim().isEmpty()) {
            portStr = ConfigSingleton.getMappingData("mail.smtp.port");
        }
        int port = 25;
        if (portStr != null && !portStr.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        String username = ConfigSingleton.getMappingData("mail.username");
        if (username == null) {
            username = ConfigSingleton.getMappingData("mail.smtp.username");
        }

        String password = ConfigSingleton.getMappingData("mail.password");
        if (password == null) {
            password = ConfigSingleton.getMappingData("mail.smtp.password");
        }

        String protocol = ConfigSingleton.getMappingData("mail.transport.protocol");
        if (protocol == null || protocol.trim().isEmpty()) {
            protocol = "smtp";
        }

        mailSender.setHost(host);
        mailSender.setPort(port);
        if (username != null) mailSender.setUsername(username);
        if (password != null) mailSender.setPassword(password);
        mailSender.setProtocol(protocol);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", ConfigSingleton.getMappingData("mail.smtp.auth") != null ? ConfigSingleton.getMappingData("mail.smtp.auth") : "true");
        props.put("mail.smtp.starttls.enable", ConfigSingleton.getMappingData("mail.smtp.starttls.enable") != null ? ConfigSingleton.getMappingData("mail.smtp.starttls.enable") : "true");
        props.put("mail.debug", ConfigSingleton.getMappingData("mail.debug") != null ? ConfigSingleton.getMappingData("mail.debug") : "false");

        log.info("Configured JavaMailSenderImpl internally with host: {}, port: {}, protocol: {}", host, port, protocol);
        return mailSender;
    }

    @Override
    public void sendMail(Mail mail) {
        if (mail == null) {
            log.warn("MailServiceImpl.sendMail called with null mail object.");
            return;
        }

        // 從 ConfigSingleton 讀取寄信者 (from)
        String from = ConfigSingleton.getMappingData("mail.from");
        if (from == null || from.trim().isEmpty()) {
            from = ConfigSingleton.getMappingData("system.mail.from");
        }
        if (from == null || from.trim().isEmpty()) {
            from = "no-reply@example.com";
        }

        log.info("MailServiceImpl.sendMail starting... From: {}, Subject: {}", from, mail.getSubject());

        try {
            // 直接於 Impl 內部建立 MailSender
            JavaMailSenderImpl mailSender = createMailSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);

            if (mail.getTo() != null && !mail.getTo().isEmpty()) {
                helper.setTo(mail.getTo().toArray(new String[0]));
            }

            if (mail.getCc() != null && !mail.getCc().isEmpty()) {
                helper.setCc(mail.getCc().toArray(new String[0]));
            }

            helper.setSubject(mail.getSubject() != null ? mail.getSubject() : "");
            helper.setText(mail.getContent() != null ? mail.getContent() : "", true);

            if (mail.getAttachmentPaths() != null && !mail.getAttachmentPaths().isEmpty()) {
                for (String path : mail.getAttachmentPaths()) {
                    File file = new File(path);
                    if (file.exists()) {
                        FileSystemResource resource = new FileSystemResource(file);
                        helper.addAttachment(resource.getFilename(), resource);
                        log.info("Added mail attachment: {}", resource.getFilename());
                    } else {
                        log.warn("Attachment file not found at path: {}", path);
                    }
                }
            }

            mailSender.send(message);
            log.info("MailServiceImpl.sendMail successfully sent email to: {}", mail.getTo());

        } catch (Exception e) {
            log.error("MailServiceImpl.sendMail failed for mail: {}", mail, e);
            throw new RuntimeException("Send mail failed", e);
        }
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java

````java
package com.example.cxfdemo.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用資料庫連線與查詢工具類別 (GenericDao)
 * 模擬主專案與外部系統之多路資料庫連線：
 * - getConnection1(): cxfdemo1 主資料庫連線 (模擬主專案 DB 連線)
 * - getConnection2(): cxfdemo2 資料庫連線 (模擬 Oracle 連線)
 * - getConnection3(): AS400 系統 A 連線 (模擬 AS400 連線)
 * - getConnection4(): AS400 系統 B 連線 (模擬 AS400 連線)
 */
public class GenericDao {

    private static final Logger log = LoggerFactory.getLogger(GenericDao.class);

    public GenericDao() {
    }

    /** 路徑 1: 主資料庫 (cxfdemo1) - 模擬主專案的 DB 連線 */
    public static Connection getConnection1() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/cxfdemo1");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection1 from java:comp/env/jdbc/cxfdemo1", e);
            return null;
        }
    }

    /** 路徑 2: 模擬 Oracle 連線 (cxfdemo2) */
    public static Connection getConnection2() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/cxfdemo2");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection2 from java:comp/env/jdbc/cxfdemo2", e);
            return null;
        }
    }

    /** 路徑 3: 模擬 AS400 系統 A 連線 (as400_a) */
    public static Connection getConnection3() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/as400_a");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection3 from java:comp/env/jdbc/as400_a", e);
            return null;
        }
    }

    /** 路徑 4: 模擬 AS400 系統 B 連線 (as400_b) */
    public static Connection getConnection4() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/as400_b");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection4 from java:comp/env/jdbc/as400_b", e);
            return null;
        }
    }

    /**
     * 查詢系統參數：SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?
     * 透過 getConnection1() 取得 cxfdemo1 主專案連線查詢
     *
     * @param propKey 欲查詢之參數名稱鍵值
     * @return 查得之 prop_value，查無資料或異常時回傳 null
     */
    public static String getSystemProperty(String propKey) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection1();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?");
                ps.setString(1, propKey);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("prop_value");
                }
            }
        } catch (Exception e) {
            log.error("Failed to query system_properties for prop_key: " + propKey, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            closeConnection(conn);
        }
        return null;
    }

    /**
     * 安全關閉資料庫連線
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failed to close connection", e);
            }
        }
    }
}
````


## File: sharedservices/src/main/java/com/example/cxfdemo/utils/WebUtils.java

````java
package com.example.cxfdemo.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class WebUtils {

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多級代理，取第一個IP為真實IP
        if (ip != null && ip.indexOf(',') != -1) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip;
    }

    public static String getClientType(HttpServletRequest request) {
        if (request == null) return "unknown";
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "unknown";

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("postman")) {
            return "Postman";
        } else if (userAgent.contains("apache-httpclient") || userAgent.contains("java") || userAgent.contains("curl")) {
            return "HttpClient";
        } else if (userAgent.contains("okhttp") || userAgent.contains("android") || userAgent.contains("iphone") || userAgent.contains("app")) {
            return "App";
        } else if (userAgent.contains("mozilla") || userAgent.contains("chrome") || userAgent.contains("safari")) {
            return "Browser";
        }
        return "Other";
    }

    public static String getLocalHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }
}
````


## File: sharedservices/src/main/java/com/example/sharedservices/config/DatabaseSystemPropertiesEnvironmentPostProcessor.java

````java
package com.example.sharedservices.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads application properties from system_properties before the application
 * context is refreshed, so regular placeholders and @Value see the DB values.
 */
public final class DatabaseSystemPropertiesEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "databaseSystemProperties";
    static final String PREFIX = "sharedservices.system-properties.";
    static final String MAIN_JNDI_DATASOURCE_PREFIX =
            "sharedservices.jndi.datasources.cxfdemo1.";
    static final String DEFAULT_QUERY = "SELECT prop_key, prop_value FROM system_properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty(PREFIX + "enabled", Boolean.class, false)) {
            return;
        }

        String query = environment.getProperty(PREFIX + "query", DEFAULT_QUERY);
        boolean failFast = environment.getProperty(PREFIX + "fail-fast", Boolean.class, true);

        try {
            Map<String, Object> properties = loadProperties(environment, query);
            environment.getPropertySources().addFirst(
                    new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        } catch (Exception ex) {
            if (failFast) {
                throw new IllegalStateException(
                        "Failed to load system_properties from the main DataSource jdbc/cxfdemo1", ex);
            }
        }
    }

    private Map<String, Object> loadProperties(ConfigurableEnvironment environment, String query)
            throws Exception {
        String jndiName = environment.getProperty(PREFIX + "jndi-name");
        if (jndiName != null) {
            return loadProperties(lookupDataSource(jndiName).getConnection(), query);
        }

        // Embedded Tomcat has not created JNDI yet at this early boot phase.
        // Use the exact same cxfdemo1 resource definition that will later be
        // registered as java:comp/env/jdbc/cxfdemo1; do not define a second DataSource.
        String configuredJndiName = requiredProperty(
                environment, MAIN_JNDI_DATASOURCE_PREFIX + "jndi-name");
        if (!"jdbc/cxfdemo1".equals(configuredJndiName)) {
            throw new IllegalStateException(MAIN_JNDI_DATASOURCE_PREFIX
                    + "jndi-name must be jdbc/cxfdemo1 because cxfdemo1 is the main database");
        }
        String url = requiredProperty(environment, MAIN_JNDI_DATASOURCE_PREFIX + "jdbc-url");
        String username = environment.getProperty(MAIN_JNDI_DATASOURCE_PREFIX + "username", "");
        String password = environment.getProperty(MAIN_JNDI_DATASOURCE_PREFIX + "password", "");
        String driverClassName = environment.getProperty(
                MAIN_JNDI_DATASOURCE_PREFIX + "driver-class-name");
        if (driverClassName != null && !driverClassName.isBlank()) {
            Class.forName(driverClassName);
        }
        return loadProperties(DriverManager.getConnection(url, username, password), query);
    }

    private String requiredProperty(ConfigurableEnvironment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured when DB properties are enabled");
        }
        return value;
    }

    private DataSource lookupDataSource(String jndiName) throws NamingException {
        InitialContext context = new InitialContext();
        try {
            Object value;
            try {
                value = context.lookup(jndiName);
            } catch (NamingException firstFailure) {
                if (jndiName.startsWith("java:")) {
                    throw firstFailure;
                }
                value = context.lookup("java:comp/env/" + jndiName);
            }
            if (!(value instanceof DataSource dataSource)) {
                throw new NamingException("JNDI object is not a DataSource: " + jndiName);
            }
            return dataSource;
        } finally {
            context.close();
        }
    }

    private Map<String, Object> loadProperties(Connection connection, String query) throws Exception {
        Map<String, Object> properties = new LinkedHashMap<>();
        try (connection;
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String key = resultSet.getString(1);
                String value = resultSet.getString(2);
                if (key != null && !key.isBlank()) {
                    properties.put(key, value == null ? "" : value);
                }
            }
        }
        return properties;
    }

    @Override
    public int getOrder() {
        // Run after Boot has loaded application.properties, but still before
        // the ApplicationContext is created and @Value is evaluated.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
````


## File: sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java

````java
package com.example.sharedservices.database;

import com.example.cxfdemo.dao.AuditLogDao;
import com.example.cxfdemo.dao.ExternalApiLogDao;
import com.example.cxfdemo.service.ApiServiceImpl;
import com.example.cxfdemo.service.MailServiceImpl;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Java configuration equivalent of the main-database section in the legacy
 * applicationContext.xml. The DAO and mapper XML remain unchanged.
 */
@AutoConfiguration
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionTemplate.class, JdbcTemplate.class})
@ConditionalOnProperty(prefix = "sharedservices.main-database", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Import({ExternalApiLogDao.class, ApiServiceImpl.class, MailServiceImpl.class})
public class MainDatabaseAutoConfiguration {

    @Bean(name = "dataSource1")
    @Primary
    @ConditionalOnMissingBean(name = "dataSource1")
    DataSource dataSource1() throws Exception {
        JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
        factory.setJndiName("jdbc/cxfdemo1");
        factory.setResourceRef(true);
        factory.setProxyInterface(DataSource.class);
        factory.setLookupOnStartup(false);
        factory.afterPropertiesSet();
        return (DataSource) factory.getObject();
    }

    @Bean(name = "sqlSessionFactory1")
    @ConditionalOnMissingBean(name = "sqlSessionFactory1")
    SqlSessionFactory sqlSessionFactory1(@Qualifier("dataSource1") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml"));
        return factory.getObject();
    }

    @Bean(name = "sqlSessionTemplate1")
    @ConditionalOnMissingBean(name = "sqlSessionTemplate1")
    SqlSessionTemplate sqlSessionTemplate1(
            @Qualifier("sqlSessionFactory1") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "jdbcTemplate1")
    @ConditionalOnMissingBean(name = "jdbcTemplate1")
    JdbcTemplate jdbcTemplate1(@Qualifier("dataSource1") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "transactionManager1")
    @ConditionalOnMissingBean(name = "transactionManager1")
    PlatformTransactionManager transactionManager1(@Qualifier("dataSource1") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(AuditLogDao.class)
    AuditLogDao auditLogDao() {
        return new AuditLogDao();
    }

}
````


## File: sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedJndiProperties.java

````java
package com.example.sharedservices.jndi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sharedservices.jndi")
public class EmbeddedJndiProperties {

    private boolean enabled;
    private Map<String, DataSourceResource> datasources = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, DataSourceResource> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, DataSourceResource> datasources) {
        this.datasources = datasources;
    }

    public static class DataSourceResource {

        private String jndiName;
        private String driverClassName;
        private String jdbcUrl;
        private String username;
        private String password;
        private int maximumPoolSize = 10;
        private int minimumIdle = 0;

        public String getJndiName() {
            return jndiName;
        }

        public void setJndiName(String jndiName) {
            this.jndiName = jndiName;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
    }
}
````


## File: sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedTomcatJndiAutoConfiguration.java

````java
package com.example.sharedservices.jndi;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.core.StandardContext;
import org.apache.naming.ContextBindings;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationListener;

@AutoConfiguration
@AutoConfigureBefore(ServletWebServerFactoryAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Tomcat.class, TomcatServletWebServerFactory.class})
@ConditionalOnProperty(prefix = "sharedservices.jndi", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EmbeddedJndiProperties.class)
public class EmbeddedTomcatJndiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ServletWebServerFactory.class)
    TomcatServletWebServerFactory jndiTomcatServletWebServerFactory(EmbeddedJndiProperties properties) {
        return new JndiTomcatServletWebServerFactory(properties);
    }

    @Bean
    ApplicationListener<WebServerInitializedEvent> bindJndiAfterWebServerStart(
            ServletWebServerFactory servletWebServerFactory) {
        return event -> {
            if (servletWebServerFactory instanceof JndiTomcatServletWebServerFactory jndiFactory
                    && event.getWebServer() instanceof TomcatWebServer tomcatWebServer) {
                jndiFactory.bindApplicationClassLoaders(tomcatWebServer);
            }
        };
    }

    private static final class JndiTomcatServletWebServerFactory
            extends TomcatServletWebServerFactory {

        private final EmbeddedJndiProperties properties;
        private final ClassLoader applicationClassLoader;

        /**
         * 保存 application.properties 綁定完成的全部 JNDI DataSource 設定，並記住啟動
         * Spring Boot 應用程式的 ClassLoader。後續要把這個 ClassLoader 綁到 Tomcat
         * Naming Context，讓主程式及外部 JAR 都能透過 InitialContext 查到同一批資源。
         */
        private JndiTomcatServletWebServerFactory(EmbeddedJndiProperties properties) {
            this.properties = properties;
            this.applicationClassLoader = Thread.currentThread().getContextClassLoader();
        }

        /**
         * 在 Spring Boot 建立 TomcatWebServer 前啟用 Tomcat Naming。沒有呼叫
         * enableNaming()，java:comp/env 下的 JNDI Context 不會建立，後續註冊的
         * jdbc/cxfdemo1 等 DataSource 也無法被 lookup。
         */
        @Override
        protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
            tomcat.enableNaming();
            return super.getTomcatWebServer(tomcat);
        }

        /**
         * Tomcat 建立 Web Application Context 時的擴充點。先保留父類別的標準處理，
         * 再註冊停止時的 ClassLoader 清理動作，最後把 properties.datasources 中的
         * 每一筆設定轉成 Tomcat ContextResource。
         */
        @Override
        protected void postProcessContext(Context context) {
            super.postProcessContext(context);
            registerNamingContextCleanup(context);
            for (Map.Entry<String, EmbeddedJndiProperties.DataSourceResource> entry
                    : properties.getDatasources().entrySet()) {
                addDataSourceResource(context, entry.getKey(), entry.getValue());
            }
        }

        /**
         * 在 Tomcat Context 停止前解除 application ClassLoader 與 Naming Context 的
         * 綁定，避免應用程式停止或重新啟動後留下舊的 JNDI Context／ClassLoader 參照。
         */
        private void registerNamingContextCleanup(Context context) {
            if (!(context instanceof StandardContext standardContext)) {
                throw new IllegalStateException(
                        "Embedded Tomcat JNDI requires a StandardContext, but found "
                                + context.getClass().getName());
            }

            context.addLifecycleListener(event -> {
                if (Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())) {
                    ContextBindings.unbindClassLoader(
                            standardContext,
                            standardContext.getNamingToken(),
                            applicationClassLoader);
                }
            });
        }

        /**
         * 將啟動 Spring Boot 應用程式的 ClassLoader 綁定到指定的 Tomcat
         * StandardContext。綁定後，由該 ClassLoader 載入的主程式或外部 JAR 執行
         * new InitialContext().lookup("java:comp/env/...") 時，才能找到此 Web 應用的資源。
         */
        private void bindApplicationClassLoader(StandardContext context) {
            try {
                ContextBindings.bindClassLoader(context, context.getNamingToken(), applicationClassLoader);
            }
            catch (Exception ex) {
                throw new IllegalStateException(
                        "Failed to bind embedded Tomcat JNDI to the application class loader", ex);
            }
        }

        /**
         * 取得目前 Tomcat Host 下的所有 Web Context，逐一呼叫
         * bindApplicationClassLoader。這個方法在 WebServerInitializedEvent 後執行，
         * 此時 Tomcat 已完成 Naming Context 的建立。
         */
        private void bindApplicationClassLoaders(TomcatWebServer tomcatWebServer) {
            for (var child : tomcatWebServer.getTomcat().getHost().findChildren()) {
                if (child instanceof StandardContext standardContext) {
                    bindApplicationClassLoader(standardContext);
                }
            }
        }

        /**
         * 將一筆 sharedservices.jndi.datasources.<id> 設定轉成 Tomcat JNDI
         * ContextResource。id（例如 cxfdemo1）只用來識別設定及產生錯誤訊息；真正
         * 註冊的 JNDI 名稱取自 settings.jndiName，實際 JDBC 位置取自
         * settings.jdbcUrl。資源由 HikariJNDIFactory 建立為可共用的單例 DataSource。
         */
        private void addDataSourceResource(Context context, String id,
                                           EmbeddedJndiProperties.DataSourceResource settings) {
            requireText(settings.getJndiName(), id + ".jndi-name");
            requireText(settings.getJdbcUrl(), id + ".jdbc-url");

            ContextResource resource = new ContextResource();
            resource.setName(settings.getJndiName());
            resource.setAuth("Container");
            resource.setType(DataSource.class.getName());
            resource.setScope("Shareable");
            resource.setSingleton(true);
            resource.setProperty("factory", "com.zaxxer.hikari.HikariJNDIFactory");
            resource.setProperty("jdbcUrl", settings.getJdbcUrl());
            resource.setProperty("username", nullToEmpty(settings.getUsername()));
            resource.setProperty("password", nullToEmpty(settings.getPassword()));
            resource.setProperty("maximumPoolSize", Integer.toString(settings.getMaximumPoolSize()));
            resource.setProperty("minimumIdle", Integer.toString(settings.getMinimumIdle()));
            if (settings.getDriverClassName() != null && !settings.getDriverClassName().isBlank()) {
                resource.setProperty("driverClassName", settings.getDriverClassName());
            }
            context.getNamingResources().addResource(resource);
        }

        /**
         * 驗證建立 JNDI DataSource 必填的文字設定。property 已包含 Map key 與欄位名，
         * 例如 cxfdemo1.jndi-name，讓啟動失敗訊息能直接指出缺少哪一項設定。
         */
        private void requireText(String value, String property) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("sharedservices.jndi.datasources." + property + " is required");
            }
        }

        /**
         * Tomcat ContextResource 的 property 不接受 null；未設定帳號或密碼時轉成空字串。
         */
        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
````

