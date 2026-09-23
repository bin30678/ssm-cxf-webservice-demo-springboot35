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
