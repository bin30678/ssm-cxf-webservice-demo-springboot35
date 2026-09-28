# SQL and MyBatis mapper resources

This file is a lossless textual snapshot. Every section contains the complete current content of the source file named in its heading.

## File: app-a/src/main/resources/db/app-a-jndi-probe.sql

````sql
CREATE TABLE IF NOT EXISTS connection_probe (
    source_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL
);
````


## File: app-a/src/main/resources/db/app-a-system-properties.sql

````sql
CREATE TABLE IF NOT EXISTS system_properties (
    prop_key VARCHAR(100) PRIMARY KEY,
    prop_value VARCHAR(255)
);

MERGE INTO system_properties (prop_key, prop_value) KEY (prop_key)
VALUES ('system.app.message', 'DB_STARTUP_PROPERTY_LOADED');

MERGE INTO system_properties (prop_key, prop_value) KEY (prop_key)
VALUES ('system.config.source', 'system_properties');

CREATE TABLE IF NOT EXISTS connection_probe (
    source_name VARCHAR(50) NOT NULL
);

DELETE FROM connection_probe;
INSERT INTO connection_probe (source_name) VALUES ('cxfdemo1');

CREATE TABLE IF NOT EXISTS policy_info (
    policy_no VARCHAR(50) PRIMARY KEY,
    holder_name VARCHAR(100),
    product_name VARCHAR(100),
    status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS audit_request (
    guid VARCHAR(50) PRIMARY KEY,
    client_ip VARCHAR(50),
    client_type VARCHAR(50),
    hostname VARCHAR(100),
    request_uri VARCHAR(255),
    request_method VARCHAR(10),
    payload CLOB
);

CREATE TABLE IF NOT EXISTS audit_response (
    guid VARCHAR(50) PRIMARY KEY,
    response_code INTEGER,
    payload CLOB
);

CREATE TABLE IF NOT EXISTS audit_fault (
    guid VARCHAR(50) PRIMARY KEY,
    error_msg CLOB
);
````


## File: app-a/src/main/resources/db/mysql-migration-support.sql

````sql
-- Run once against the existing local MySQL fixture used by the SSM demo.
INSERT INTO cxfdemo1.system_properties (prop_key, prop_value)
VALUES ('system.app.message', 'MYSQL_SYSTEM_PROPERTIES_LOADED'),
       ('system.config.source', 'mysql_system_properties')
ON DUPLICATE KEY UPDATE prop_value = VALUES(prop_value);

CREATE TABLE IF NOT EXISTS cxfdemo1.connection_probe (
    source_name VARCHAR(50) NOT NULL
);
INSERT INTO cxfdemo1.connection_probe (source_name)
SELECT 'cxfdemo1' WHERE NOT EXISTS (SELECT 1 FROM cxfdemo1.connection_probe);

CREATE TABLE IF NOT EXISTS cxfdemo2.connection_probe (
    source_name VARCHAR(50) NOT NULL
);
INSERT INTO cxfdemo2.connection_probe (source_name)
SELECT 'cxfdemo2' WHERE NOT EXISTS (SELECT 1 FROM cxfdemo2.connection_probe);
````


## File: sharedservices/src/main/resources/mapper/AuditLog.xml

````xml
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
````


## File: sharedservices/src/main/resources/mapper/ExternalApiLog.xml

````xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="ExternalApiLog">

    <insert id="insertApiLog" parameterType="map">
        INSERT INTO external_api_log (url, param, response_body, exception_msg, call_time)
        VALUES (#{url}, #{requestParam}, #{responseBody}, #{exceptionMsg}, #{callTime})
    </insert>

</mapper>
````

