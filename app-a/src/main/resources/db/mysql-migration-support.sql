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
