# 服務層介面與商業邏輯實作 (Service Layer)

本文件收錄核心商業邏輯服務介面及其具體實作類別（包括外部 API 呼叫服務 ApiService 與郵件發送服務 MailService）。完整保留所有業務處理與錯誤控管邏輯。

> 本文件收錄 4 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: sharedservices/src/main/java/com/example/cxfdemo/service/ApiService.java

```java
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

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java

```java
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

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java

```java
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

```

---

## File: sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java

```java
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

```

---
