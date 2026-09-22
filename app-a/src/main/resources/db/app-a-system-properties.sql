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
