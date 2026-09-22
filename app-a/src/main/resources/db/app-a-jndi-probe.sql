CREATE TABLE IF NOT EXISTS connection_probe (
    source_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL
);
