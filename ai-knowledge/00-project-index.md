# 專案結構與原始碼索引導引 (Project Structure & Source Index)

本知識庫專為 **Microsoft 365 Copilot** 與 AI 助理設計，將 `ssm-cxf-webservice-demo-springboot35` 專案之完整原始碼與設定檔在不失真、不摘要的前提下，封裝為易於檢索與關聯分析的 Markdown 文件集。

---

# Project Structure

本專案為基於 **Spring Boot 3.5.16**、**Java 21**、**Apache CXF 4.1.4** 與 **MyBatis** 的多模組 (Multi-Module) Maven 企業級 Web 服務示範專案。

模組組織結構與核心職責如下：

- **root (`ssm-cxf-webservice-demo-springboot35`)**: 專案根目錄，包含父層 `pom.xml`，統一宣告 Spring Boot Starter Parent、依賴版本管理 (cxf-bom) 與 Java 21 編譯配置。
- **`common-core`**: 核心基礎共通模組，提供跨模組共用之基礎類別與標記。
- **`sharedservices`**: 共通服務與自動配置模組，包含核心 Service (ApiService, MailService)、DAO 實作、CXF Interceptor、自訂 AutoConfiguration (JNDI, Database, Audit, Environment Listener)、EnvironmentPostProcessor 以及 MyBatis Mapper XML。
- **`app-a`**: 主應用程式模組 A，負責提供 JAX-RS RESTful 端點 (/health, /nouternal)、JAX-WS SOAP 服務、JNDI 與外部函式庫整合自我診斷探針。
- **`app-b`**: 排程與批次處理模組 B，負責整合 Spring @Scheduled 與 Quartz 定時任務，執行背景作業。
- **`app-c`**: 獨立微服務模組 C，Spring Boot 3.5 基礎應用程式。
- **`app-d`**: 獨立微服務模組 D，Spring Boot 3.5 基礎應用程式。

---

# Source Index

全專案共收錄 **69** 個原始程式碼與設定檔案。下表記錄每個檔案的相對路徑、所屬模組、檔案用途類型、類別/介面名稱與所在之 Knowledge Markdown 文件：

| Source File (Relative Path) | Module | File Type | Class / Interface | Knowledge File |
| :--- | :--- | :--- | :--- | :--- |
| `app-a/pom.xml` | `app-a` | Maven POM | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-a/src/main/java/com/example/appa/AppAApplication.java` | `app-a` | Spring Boot Application | `AppAApplication` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `app-a/src/main/java/com/example/appa/AppAExternalLibAJndiProbe.java` | `app-a` | Startup Probe (Runner) | `AppAExternalLibAJndiProbe` | [10-common-utility.md](file:///10-common-utility.md) |
| `app-a/src/main/java/com/example/appa/AppAJndiConnectionProbe.java` | `app-a` | Startup Probe (Runner) | `AppAJndiConnectionProbe` | [10-common-utility.md](file:///10-common-utility.md) |
| `app-a/src/main/java/com/example/appa/AppAPropertyProbe.java` | `app-a` | Startup Probe (Runner) | `AppAPropertyProbe` | [10-common-utility.md](file:///10-common-utility.md) |
| `app-a/src/main/java/com/example/appa/config/AppACxfConfiguration.java` | `app-a` | Spring Configuration | `AppACxfConfiguration` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDao.java` | `app-a` | DAO Interface | `PolicyDao` | [04-dao.md](file:///04-dao.md) |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | `app-a` | DAO Implementation | `PolicyDaoImpl` | [04-dao.md](file:///04-dao.md) |
| `app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java` | `app-a` | Domain Model (JAXB) | `PolicyInfo` | [09-model-and-pojo.md](file:///09-model-and-pojo.md) |
| `app-a/src/main/java/com/example/cxfdemo/rest/HealthResource.java` | `app-a` | JAX-RS Resource | `HealthResource` | [02-api-resource.md](file:///02-api-resource.md) |
| `app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java` | `app-a` | JAX-RS Resource | `MainNouternalResource` | [02-api-resource.md](file:///02-api-resource.md) |
| `app-a/src/main/resources/application-mysql.properties` | `app-a` | Configuration Properties | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-a/src/main/resources/application.properties` | `app-a` | Configuration Properties | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-a/src/main/resources/db/app-a-jndi-probe.sql` | `app-a` | Database SQL | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-a/src/main/resources/db/app-a-system-properties.sql` | `app-a` | Database SQL | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-a/src/main/resources/db/mysql-migration-support.sql` | `app-a` | Database SQL | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-a/src/test/java/com/example/appa/AuditProbePort.java` | `app-a` | JAX-WS Port (Interface) | `AuditProbePort` | [02-api-resource.md](file:///02-api-resource.md) |
| `app-a/src/test/java/com/example/appa/AuditProbePortImpl.java` | `app-a` | JAX-WS Port (Implementation) | `AuditProbePortImpl` | [02-api-resource.md](file:///02-api-resource.md) |
| `app-a/src/test/java/com/example/appa/BaseDaoMainDatabaseTest.java` | `app-a` | Database Integration Test | `BaseDaoMainDatabaseTest` | [11-test-suite.md](file:///11-test-suite.md) |
| `app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java` | `app-a` | CXF Interceptor Test | `CxfAuditInterceptorIntegrationTest` | [11-test-suite.md](file:///11-test-suite.md) |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `app-a` | Startup Integration Test | `DatabaseSystemPropertiesStartupTest` | [11-test-suite.md](file:///11-test-suite.md) |
| `app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java` | `app-a` | Resource Integration Test | `MainNouternalResourceIntegrationTest` | [11-test-suite.md](file:///11-test-suite.md) |
| `app-a/src/test/java/com/example/cxfdemo/rest/ScannedProbeResource.java` | `app-a` | JAX-RS Resource (Test Probe) | `ScannedProbeResource` | [02-api-resource.md](file:///02-api-resource.md) |
| `app-b/pom.xml` | `app-b` | Maven POM | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-b/src/main/java/com/example/appb/AppBApplication.java` | `app-b` | Spring Boot Application | `AppBApplication` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `app-b/src/main/java/com/example/appb/config/AppBSchedulingConfiguration.java` | `app-b` | Spring Configuration | `AppBSchedulingConfiguration` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `app-b/src/main/java/com/example/cxfdemo/scheduler/QuartzDemoJob.java` | `app-b` | Quartz Job | `QuartzDemoJob` | [08-scheduler.md](file:///08-scheduler.md) |
| `app-b/src/main/java/com/example/cxfdemo/scheduler/ScheduledTasks.java` | `app-b` | Spring Scheduled Task | `ScheduledTasks` | [08-scheduler.md](file:///08-scheduler.md) |
| `app-b/src/main/resources/application.properties` | `app-b` | Configuration Properties | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-b/src/test/java/com/example/appb/AppBSchedulingIntegrationTest.java` | `app-b` | Scheduling Integration Test | `AppBSchedulingIntegrationTest` | [11-test-suite.md](file:///11-test-suite.md) |
| `app-b/src/test/java/com/example/appb/ScheduledTasksTest.java` | `app-b` | Scheduled Task Unit Test | `ScheduledTasksTest` | [11-test-suite.md](file:///11-test-suite.md) |
| `app-c/pom.xml` | `app-c` | Maven POM | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-c/src/main/java/com/example/appc/AppCApplication.java` | `app-c` | Spring Boot Application | `AppCApplication` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `app-d/pom.xml` | `app-d` | Maven POM | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `app-d/src/main/java/com/example/appd/AppDApplication.java` | `app-d` | Spring Boot Application | `AppDApplication` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `common-core/pom.xml` | `common-core` | Maven POM | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `common-core/src/main/java/com/example/commoncore/CommonCoreMarker.java` | `common-core` | Module Marker | `CommonCoreMarker` | [10-common-utility.md](file:///10-common-utility.md) |
| `pom.xml` | `root` | Maven POM | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `sharedservices/pom.xml` | `sharedservices` | Maven POM | `-` | [01-maven-and-config.md](file:///01-maven-and-config.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java` | `sharedservices` | Legacy Config Singleton | `ConfigSingleton` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java` | `sharedservices` | DAO Implementation | `AuditLogDao` | [04-dao.md](file:///04-dao.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java` | `sharedservices` | DAO Base Class | `BaseDao` | [04-dao.md](file:///04-dao.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java` | `sharedservices` | DAO Implementation | `ExternalApiLogDao` | [04-dao.md](file:///04-dao.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/fault/ApiError.java` | `sharedservices` | Fault Model / DTO | `ApiError` | [07-cxf.md](file:///07-cxf.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java` | `sharedservices` | Fault Exception | `ServiceFaultException` | [07-cxf.md](file:///07-cxf.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditFaultInterceptor.java` | `sharedservices` | CXF Interceptor | `AuditFaultInterceptor` | [07-cxf.md](file:///07-cxf.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | `sharedservices` | CXF Interceptor | `AuditRequestInterceptor` | [07-cxf.md](file:///07-cxf.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditResponseInterceptor.java` | `sharedservices` | CXF Interceptor | `AuditResponseInterceptor` | [07-cxf.md](file:///07-cxf.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/UnifiedFaultInterceptor.java` | `sharedservices` | CXF Interceptor | `UnifiedFaultInterceptor` | [07-cxf.md](file:///07-cxf.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/listener/FontCheckListener.java` | `sharedservices` | ServletContextListener | `FontCheckListener` | [10-common-utility.md](file:///10-common-utility.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/listener/TiffImageReaderCheckListener.java` | `sharedservices` | ServletContextListener | `TiffImageReaderCheckListener` | [10-common-utility.md](file:///10-common-utility.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/model/Mail.java` | `sharedservices` | Domain Model (DTO) | `Mail` | [09-model-and-pojo.md](file:///09-model-and-pojo.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/provider/GsonProvider.java` | `sharedservices` | CXF Provider | `GsonProvider` | [07-cxf.md](file:///07-cxf.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/service/ApiService.java` | `sharedservices` | Service Interface | `ApiService` | [03-service-01.md](file:///03-service-01.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java` | `sharedservices` | Service Implementation | `ApiServiceImpl` | [03-service-01.md](file:///03-service-01.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java` | `sharedservices` | Service Interface | `MailService` | [03-service-01.md](file:///03-service-01.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java` | `sharedservices` | Service Implementation | `MailServiceImpl` | [03-service-01.md](file:///03-service-01.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java` | `sharedservices` | DAO Generic Utility | `GenericDao` | [04-dao.md](file:///04-dao.md) |
| `sharedservices/src/main/java/com/example/cxfdemo/utils/WebUtils.java` | `sharedservices` | Web Utility | `WebUtils` | [10-common-utility.md](file:///10-common-utility.md) |
| `sharedservices/src/main/java/com/example/sharedservices/config/DatabaseSystemPropertiesEnvironmentPostProcessor.java` | `sharedservices` | EnvironmentPostProcessor | `DatabaseSystemPropertiesEnvironmentPostProcessor` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java` | `sharedservices` | AutoConfiguration | `CxfAuditInterceptorAutoConfiguration` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java` | `sharedservices` | AutoConfiguration | `MainDatabaseAutoConfiguration` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedJndiProperties.java` | `sharedservices` | Configuration Properties POJO | `EmbeddedJndiProperties` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedTomcatJndiAutoConfiguration.java` | `sharedservices` | AutoConfiguration | `EmbeddedTomcatJndiAutoConfiguration` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/java/com/example/sharedservices/listener/SystemEnvironmentListenerAutoConfiguration.java` | `sharedservices` | AutoConfiguration | `SystemEnvironmentListenerAutoConfiguration` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/resources/META-INF/spring.factories` | `sharedservices` | Spring Factories SPI | `-` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | `sharedservices` | Spring AutoConfiguration Imports SPI | `-` | [06-spring-configuration.md](file:///06-spring-configuration.md) |
| `sharedservices/src/main/resources/mapper/AuditLog.xml` | `sharedservices` | MyBatis Mapper XML | `-` | [05-mybatis-mapper.md](file:///05-mybatis-mapper.md) |
| `sharedservices/src/main/resources/mapper/ExternalApiLog.xml` | `sharedservices` | MyBatis Mapper XML | `-` | [05-mybatis-mapper.md](file:///05-mybatis-mapper.md) |

---

# Architecture Index

依架構元件職責角色將專案各元件分類如下（未明確定界或輔助檔歸類於 other，不進行揣測）：

### JAX-RS Resource

- **`app-a/src/main/java/com/example/cxfdemo/rest/HealthResource.java`** (`HealthResource`) [app-a] → 參見 [02-api-resource.md](file:///02-api-resource.md)
- **`app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java`** (`MainNouternalResource`) [app-a] → 參見 [02-api-resource.md](file:///02-api-resource.md)
- **`app-a/src/test/java/com/example/appa/AuditProbePort.java`** (`AuditProbePort`) [app-a] → 參見 [02-api-resource.md](file:///02-api-resource.md)
- **`app-a/src/test/java/com/example/appa/AuditProbePortImpl.java`** (`AuditProbePortImpl`) [app-a] → 參見 [02-api-resource.md](file:///02-api-resource.md)
- **`app-a/src/test/java/com/example/cxfdemo/rest/ScannedProbeResource.java`** (`ScannedProbeResource`) [app-a] → 參見 [02-api-resource.md](file:///02-api-resource.md)

### Spring Controller

*（目前無 Spring Controller 元件）*

### Service

- **`sharedservices/src/main/java/com/example/cxfdemo/service/ApiService.java`** (`ApiService`) [sharedservices] → 參見 [03-service-01.md](file:///03-service-01.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java`** (`ApiServiceImpl`) [sharedservices] → 參見 [03-service-01.md](file:///03-service-01.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java`** (`MailService`) [sharedservices] → 參見 [03-service-01.md](file:///03-service-01.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java`** (`MailServiceImpl`) [sharedservices] → 參見 [03-service-01.md](file:///03-service-01.md)

### DAO

- **`app-a/src/main/java/com/example/cxfdemo/dao/PolicyDao.java`** (`PolicyDao`) [app-a] → 參見 [04-dao.md](file:///04-dao.md)
- **`app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java`** (`PolicyDaoImpl`) [app-a] → 參見 [04-dao.md](file:///04-dao.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java`** (`AuditLogDao`) [sharedservices] → 參見 [04-dao.md](file:///04-dao.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java`** (`BaseDao`) [sharedservices] → 參見 [04-dao.md](file:///04-dao.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java`** (`ExternalApiLogDao`) [sharedservices] → 參見 [04-dao.md](file:///04-dao.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java`** (`GenericDao`) [sharedservices] → 參見 [04-dao.md](file:///04-dao.md)

### MyBatis Mapper

- **`sharedservices/src/main/resources/mapper/AuditLog.xml`** [sharedservices] → 參見 [05-mybatis-mapper.md](file:///05-mybatis-mapper.md)
- **`sharedservices/src/main/resources/mapper/ExternalApiLog.xml`** [sharedservices] → 參見 [05-mybatis-mapper.md](file:///05-mybatis-mapper.md)

### Spring Configuration

- **`app-a/src/main/java/com/example/appa/AppAApplication.java`** (`AppAApplication`) [app-a] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`app-a/src/main/java/com/example/appa/config/AppACxfConfiguration.java`** (`AppACxfConfiguration`) [app-a] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`app-a/src/main/resources/application-mysql.properties`** [app-a] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-a/src/main/resources/application.properties`** [app-a] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-b/src/main/java/com/example/appb/AppBApplication.java`** (`AppBApplication`) [app-b] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`app-b/src/main/java/com/example/appb/config/AppBSchedulingConfiguration.java`** (`AppBSchedulingConfiguration`) [app-b] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`app-b/src/main/resources/application.properties`** [app-b] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-c/src/main/java/com/example/appc/AppCApplication.java`** (`AppCApplication`) [app-c] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`app-d/src/main/java/com/example/appd/AppDApplication.java`** (`AppDApplication`) [app-d] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedJndiProperties.java`** (`EmbeddedJndiProperties`) [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)

### AutoConfiguration

- **`sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java`** (`CxfAuditInterceptorAutoConfiguration`) [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java`** (`MainDatabaseAutoConfiguration`) [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedTomcatJndiAutoConfiguration.java`** (`EmbeddedTomcatJndiAutoConfiguration`) [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`sharedservices/src/main/java/com/example/sharedservices/listener/SystemEnvironmentListenerAutoConfiguration.java`** (`SystemEnvironmentListenerAutoConfiguration`) [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`sharedservices/src/main/resources/META-INF/spring.factories`** [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`sharedservices/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`** [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)

### EnvironmentPostProcessor

- **`sharedservices/src/main/java/com/example/sharedservices/config/DatabaseSystemPropertiesEnvironmentPostProcessor.java`** (`DatabaseSystemPropertiesEnvironmentPostProcessor`) [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)

### CXF Interceptor

- **`sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditFaultInterceptor.java`** (`AuditFaultInterceptor`) [sharedservices] → 參見 [07-cxf.md](file:///07-cxf.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java`** (`AuditRequestInterceptor`) [sharedservices] → 參見 [07-cxf.md](file:///07-cxf.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditResponseInterceptor.java`** (`AuditResponseInterceptor`) [sharedservices] → 參見 [07-cxf.md](file:///07-cxf.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/interceptor/UnifiedFaultInterceptor.java`** (`UnifiedFaultInterceptor`) [sharedservices] → 參見 [07-cxf.md](file:///07-cxf.md)

### Provider

- **`sharedservices/src/main/java/com/example/cxfdemo/provider/GsonProvider.java`** (`GsonProvider`) [sharedservices] → 參見 [07-cxf.md](file:///07-cxf.md)

### Scheduler

- **`app-b/src/main/java/com/example/cxfdemo/scheduler/QuartzDemoJob.java`** (`QuartzDemoJob`) [app-b] → 參見 [08-scheduler.md](file:///08-scheduler.md)
- **`app-b/src/main/java/com/example/cxfdemo/scheduler/ScheduledTasks.java`** (`ScheduledTasks`) [app-b] → 參見 [08-scheduler.md](file:///08-scheduler.md)

### Utility

- **`sharedservices/src/main/java/com/example/cxfdemo/utils/WebUtils.java`** (`WebUtils`) [sharedservices] → 參見 [10-common-utility.md](file:///10-common-utility.md)

### POJO / DTO

- **`app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java`** (`PolicyInfo`) [app-a] → 參見 [09-model-and-pojo.md](file:///09-model-and-pojo.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/fault/ApiError.java`** (`ApiError`) [sharedservices] → 參見 [07-cxf.md](file:///07-cxf.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/model/Mail.java`** (`Mail`) [sharedservices] → 參見 [09-model-and-pojo.md](file:///09-model-and-pojo.md)

### Maven Configuration

- **`app-a/pom.xml`** [app-a] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-b/pom.xml`** [app-b] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-c/pom.xml`** [app-c] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-d/pom.xml`** [app-d] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`common-core/pom.xml`** [common-core] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`pom.xml`** [root] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`sharedservices/pom.xml`** [sharedservices] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)

### other

- **`app-a/src/main/java/com/example/appa/AppAExternalLibAJndiProbe.java`** (`AppAExternalLibAJndiProbe`) [app-a] → 參見 [10-common-utility.md](file:///10-common-utility.md)
- **`app-a/src/main/java/com/example/appa/AppAJndiConnectionProbe.java`** (`AppAJndiConnectionProbe`) [app-a] → 參見 [10-common-utility.md](file:///10-common-utility.md)
- **`app-a/src/main/java/com/example/appa/AppAPropertyProbe.java`** (`AppAPropertyProbe`) [app-a] → 參見 [10-common-utility.md](file:///10-common-utility.md)
- **`app-a/src/main/resources/db/app-a-jndi-probe.sql`** [app-a] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-a/src/main/resources/db/app-a-system-properties.sql`** [app-a] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-a/src/main/resources/db/mysql-migration-support.sql`** [app-a] → 參見 [01-maven-and-config.md](file:///01-maven-and-config.md)
- **`app-a/src/test/java/com/example/appa/BaseDaoMainDatabaseTest.java`** (`BaseDaoMainDatabaseTest`) [app-a] → 參見 [11-test-suite.md](file:///11-test-suite.md)
- **`app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java`** (`CxfAuditInterceptorIntegrationTest`) [app-a] → 參見 [11-test-suite.md](file:///11-test-suite.md)
- **`app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java`** (`DatabaseSystemPropertiesStartupTest`) [app-a] → 參見 [11-test-suite.md](file:///11-test-suite.md)
- **`app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java`** (`MainNouternalResourceIntegrationTest`) [app-a] → 參見 [11-test-suite.md](file:///11-test-suite.md)
- **`app-b/src/test/java/com/example/appb/AppBSchedulingIntegrationTest.java`** (`AppBSchedulingIntegrationTest`) [app-b] → 參見 [11-test-suite.md](file:///11-test-suite.md)
- **`app-b/src/test/java/com/example/appb/ScheduledTasksTest.java`** (`ScheduledTasksTest`) [app-b] → 參見 [11-test-suite.md](file:///11-test-suite.md)
- **`common-core/src/main/java/com/example/commoncore/CommonCoreMarker.java`** (`CommonCoreMarker`) [common-core] → 參見 [10-common-utility.md](file:///10-common-utility.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java`** (`ConfigSingleton`) [sharedservices] → 參見 [06-spring-configuration.md](file:///06-spring-configuration.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java`** (`ServiceFaultException`) [sharedservices] → 參見 [07-cxf.md](file:///07-cxf.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/listener/FontCheckListener.java`** (`FontCheckListener`) [sharedservices] → 參見 [10-common-utility.md](file:///10-common-utility.md)
- **`sharedservices/src/main/java/com/example/cxfdemo/listener/TiffImageReaderCheckListener.java`** (`TiffImageReaderCheckListener`) [sharedservices] → 參見 [10-common-utility.md](file:///10-common-utility.md)
