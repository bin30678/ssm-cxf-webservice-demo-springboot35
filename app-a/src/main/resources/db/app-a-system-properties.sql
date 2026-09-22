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
