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
