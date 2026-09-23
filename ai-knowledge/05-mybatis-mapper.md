# MyBatis XML 映射檔 (MyBatis Mappers)

本文件包含系統中的 MyBatis XML Mapper 設定檔，記錄所有 SQL 語句、結果映射 resultMap 與參數定義，完全保留原始 XML 標籤與內容。

> 本文件收錄 2 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: sharedservices/src/main/resources/mapper/AuditLog.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="AuditLog">

    <insert id="insertRequestLog" parameterType="map">
        INSERT INTO audit_request (guid, client_ip, client_type, hostname, request_uri, request_method, payload)
        VALUES (#{guid}, #{ip}, #{clientType}, #{hostname}, #{uri}, #{method}, #{payload})
        ON DUPLICATE KEY UPDATE client_ip = VALUES(client_ip), request_uri = VALUES(request_uri), payload = VALUES(payload)
    </insert>

    <insert id="insertResponseLog" parameterType="map">
        INSERT INTO audit_response (guid, response_code, payload)
        VALUES (#{guid}, #{responseCode}, #{payload})
        ON DUPLICATE KEY UPDATE response_code = VALUES(response_code), payload = VALUES(payload)
    </insert>

    <insert id="insertFaultLog" parameterType="map">
        INSERT INTO audit_fault (guid, error_msg)
        VALUES (#{guid}, #{errorMsg})
        ON DUPLICATE KEY UPDATE error_msg = VALUES(error_msg)
    </insert>

</mapper>

```

---

## File: sharedservices/src/main/resources/mapper/ExternalApiLog.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="ExternalApiLog">

    <insert id="insertApiLog" parameterType="map">
        INSERT INTO external_api_log (url, param, response_body, exception_msg, call_time)
        VALUES (#{url}, #{requestParam}, #{responseBody}, #{exceptionMsg}, #{callTime})
    </insert>

</mapper>

```

---
