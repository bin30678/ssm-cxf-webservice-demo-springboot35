# Maven 專案物件模型與環境設定檔 (Maven & Application Configuration)

本文件包含整個多模組 Maven 專案的根 pom.xml、各子模組 pom.xml、Spring Boot 應用程式環境屬性設定檔 (application*.properties) 以及資料庫初始化與 JNDI Probe SQL 腳本。所有原始設定均原封不動完整保留。

> 本文件收錄 13 個原始程式碼與設定檔案，內容皆完全保持原檔不變。

---

## File: app-a/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ssm-cxf-webservice-demo-springboot35</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>app-a</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>sharedservices</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.external</groupId>
            <artifactId>external-lib-a</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.cxf</groupId>
            <artifactId>cxf-spring-boot-starter-jaxrs</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.cxf</groupId>
            <artifactId>cxf-spring-boot-starter-jaxws</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

```

---

## File: app-a/src/main/resources/application-mysql.properties

```properties
server.port=18080

sharedservices.jndi.datasources.cxfdemo1.driver-class-name=com.mysql.cj.jdbc.Driver
sharedservices.jndi.datasources.cxfdemo1.jdbc-url=jdbc:mysql://localhost:3306/cxfdemo1?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei
sharedservices.jndi.datasources.cxfdemo1.username=${MYSQL_USERNAME:root}
sharedservices.jndi.datasources.cxfdemo1.password=${MYSQL_PASSWORD:123456}

sharedservices.jndi.datasources.cxfdemo2.driver-class-name=com.mysql.cj.jdbc.Driver
sharedservices.jndi.datasources.cxfdemo2.jdbc-url=jdbc:mysql://localhost:3306/cxfdemo2?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei
sharedservices.jndi.datasources.cxfdemo2.username=${MYSQL_USERNAME:root}
sharedservices.jndi.datasources.cxfdemo2.password=${MYSQL_PASSWORD:123456}

# The original SSM demo declares separate AS400 JNDI names. The local MySQL
# fixture keeps those names but points them at cxfdemo2, which contains the
# external-library test tables.
sharedservices.jndi.datasources.as400-a.driver-class-name=com.mysql.cj.jdbc.Driver
sharedservices.jndi.datasources.as400-a.jdbc-url=jdbc:mysql://localhost:3306/cxfdemo2?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei
sharedservices.jndi.datasources.as400-a.username=${MYSQL_USERNAME:root}
sharedservices.jndi.datasources.as400-a.password=${MYSQL_PASSWORD:123456}

sharedservices.jndi.datasources.as400-b.driver-class-name=com.mysql.cj.jdbc.Driver
sharedservices.jndi.datasources.as400-b.jdbc-url=jdbc:mysql://localhost:3306/cxfdemo2?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei
sharedservices.jndi.datasources.as400-b.username=${MYSQL_USERNAME:root}
sharedservices.jndi.datasources.as400-b.password=${MYSQL_PASSWORD:123456}

sharedservices.jndi.datasources.as400-c.driver-class-name=com.mysql.cj.jdbc.Driver
sharedservices.jndi.datasources.as400-c.jdbc-url=jdbc:mysql://localhost:3306/cxfdemo2?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei
sharedservices.jndi.datasources.as400-c.username=${MYSQL_USERNAME:root}
sharedservices.jndi.datasources.as400-c.password=${MYSQL_PASSWORD:123456}

```

---

## File: app-a/src/main/resources/application.properties

```properties
spring.application.name=app-a
server.port=0
server.servlet.context-parameters.targetFont=\u6a19\u6977\u9ad4

# Spring discovers @Path / @Provider beans; CXF publishes them without a resource list.
cxf.jaxrs.component-scan=true
cxf.jaxrs.server.path=/

sharedservices.system-properties.enabled=true
sharedservices.system-properties.fail-fast=true

# These placeholders are resolved after sharedservices adds DB values to Environment.
app-a.application-message=${system.app.message}
app-a.config-source=${system.config.source}

# Embedded Tomcat JNDI resources. For MySQL, change driver-class-name, jdbc-url,
# username and password; GenericDao and external JAR lookup names stay unchanged.
sharedservices.jndi.enabled=true

sharedservices.jndi.datasources.cxfdemo1.jndi-name=jdbc/cxfdemo1
sharedservices.jndi.datasources.cxfdemo1.driver-class-name=org.h2.Driver
sharedservices.jndi.datasources.cxfdemo1.jdbc-url=jdbc:h2:mem:cxfdemo1;MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:db/app-a-system-properties.sql'
sharedservices.jndi.datasources.cxfdemo1.username=sa
sharedservices.jndi.datasources.cxfdemo1.password=
sharedservices.jndi.datasources.cxfdemo1.maximum-pool-size=3

sharedservices.jndi.datasources.cxfdemo2.jndi-name=jdbc/cxfdemo2
sharedservices.jndi.datasources.cxfdemo2.driver-class-name=org.h2.Driver
sharedservices.jndi.datasources.cxfdemo2.jdbc-url=jdbc:h2:mem:cxfdemo2;MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:db/app-a-jndi-probe.sql'
sharedservices.jndi.datasources.cxfdemo2.username=sa
sharedservices.jndi.datasources.cxfdemo2.password=
sharedservices.jndi.datasources.cxfdemo2.maximum-pool-size=3

sharedservices.jndi.datasources.as400-a.jndi-name=jdbc/as400_a
sharedservices.jndi.datasources.as400-a.driver-class-name=org.h2.Driver
sharedservices.jndi.datasources.as400-a.jdbc-url=jdbc:h2:mem:as400_a;MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:db/app-a-jndi-probe.sql'
sharedservices.jndi.datasources.as400-a.username=sa
sharedservices.jndi.datasources.as400-a.password=
sharedservices.jndi.datasources.as400-a.maximum-pool-size=3

sharedservices.jndi.datasources.as400-b.jndi-name=jdbc/as400_b
sharedservices.jndi.datasources.as400-b.driver-class-name=org.h2.Driver
sharedservices.jndi.datasources.as400-b.jdbc-url=jdbc:h2:mem:as400_b;MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:db/app-a-jndi-probe.sql'
sharedservices.jndi.datasources.as400-b.username=sa
sharedservices.jndi.datasources.as400-b.password=
sharedservices.jndi.datasources.as400-b.maximum-pool-size=3

sharedservices.jndi.datasources.as400-c.jndi-name=jdbc/as400_c
sharedservices.jndi.datasources.as400-c.driver-class-name=org.h2.Driver
sharedservices.jndi.datasources.as400-c.jdbc-url=jdbc:h2:mem:as400_c;MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:db/app-a-jndi-probe.sql'
sharedservices.jndi.datasources.as400-c.username=sa
sharedservices.jndi.datasources.as400-c.password=
sharedservices.jndi.datasources.as400-c.maximum-pool-size=3

```

---

## File: app-a/src/main/resources/db/app-a-jndi-probe.sql

```sql
CREATE TABLE IF NOT EXISTS connection_probe (
    source_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL
);

```

---

## File: app-a/src/main/resources/db/app-a-system-properties.sql

```sql
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

```

---

## File: app-a/src/main/resources/db/mysql-migration-support.sql

```sql
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

```

---

## File: app-b/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.example</groupId><artifactId>ssm-cxf-webservice-demo-springboot35</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>app-b</artifactId>
    <dependencies>
        <dependency><groupId>com.example</groupId><artifactId>common-core</artifactId><version>${project.version}</version></dependency>
        <dependency><groupId>com.example</groupId><artifactId>sharedservices</artifactId><version>${project.version}</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-quartz</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>

```

---

## File: app-b/src/main/resources/application.properties

```properties
spring.application.name=app-b
spring.main.web-application-type=none
spring.quartz.job-store-type=memory

```

---

## File: app-c/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.example</groupId><artifactId>ssm-cxf-webservice-demo-springboot35</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>app-c</artifactId>
    <dependencies>
        <dependency><groupId>com.example</groupId><artifactId>common-core</artifactId><version>${project.version}</version></dependency>
        <dependency><groupId>com.example</groupId><artifactId>sharedservices</artifactId><version>${project.version}</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>

```

---

## File: app-d/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.example</groupId><artifactId>ssm-cxf-webservice-demo-springboot35</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>app-d</artifactId>
    <dependencies>
        <dependency><groupId>com.example</groupId><artifactId>common-core</artifactId><version>${project.version}</version></dependency>
        <dependency><groupId>com.example</groupId><artifactId>sharedservices</artifactId><version>${project.version}</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>

```

---

## File: common-core/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ssm-cxf-webservice-demo-springboot35</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>common-core</artifactId>
</project>

```

---

## File: pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>ssm-cxf-webservice-demo-springboot35</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>ssm-cxf-webservice-demo-springboot35</name>

    <modules>
        <module>common-core</module>
        <module>sharedservices</module>
        <module>app-a</module>
        <module>app-b</module>
        <module>app-c</module>
        <module>app-d</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
        <cxf.version>4.1.4</cxf.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.apache.cxf</groupId>
                <artifactId>cxf-bom</artifactId>
                <version>${cxf.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>

```

---

## File: sharedservices/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>ssm-cxf-webservice-demo-springboot35</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>sharedservices</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.5.2</version>
            <exclusions>
                <exclusion>
                    <groupId>commons-logging</groupId>
                    <artifactId>commons-logging</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.cxf</groupId>
            <artifactId>cxf-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.cxf</groupId>
            <artifactId>cxf-rt-bindings-soap</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.cxf</groupId>
            <artifactId>cxf-rt-transports-http</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.cxf</groupId>
            <artifactId>cxf-rt-frontend-jaxrs</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-spring</artifactId>
            <version>3.0.5</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.5.19</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
    </dependencies>
</project>

```

---
