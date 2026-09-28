# Existing project documentation

This file is a lossless textual snapshot. Every section contains the complete current content of the source file named in its heading.

## File: README.md

````markdown
# SSM CXF WebService Demo - Spring Boot migration

This is the multi-module Spring Boot 3.5.16 / Apache CXF 4.1.4 / Java 21 migration target.

```text
ssm-cxf-webservice-demo-springboot35
├── common-core
├── sharedservices
├── app-a
├── app-b
├── app-c
└── app-d
```

`sharedservices` registers a Spring Boot `EnvironmentPostProcessor`. During standalone Boot
startup it uses the `sharedservices.jndi.datasources.cxfdemo1.*` definition, queries
`system_properties`, and adds all `prop_key` / `prop_value` rows as a high-priority Spring property
source. This is the same definition later registered as JNDI `jdbc/cxfdemo1`; there is no separate
`spring.datasource.*` configuration. An application server that has already created JNDI before
Spring starts can be selected explicitly with `sharedservices.system-properties.jndi-name`.

`app-a` is directly runnable with an H2 demonstration database. Its integration test starts the
real Boot application without creating a fake JNDI context, verifies both placeholder mechanisms,
then obtains Boot's `DataSource` bean and queries the same table.

When `sharedservices.jndi.enabled=true`, `sharedservices` enables naming in embedded Tomcat and
registers four application-scoped resources: `jdbc/cxfdemo1`, `jdbc/cxfdemo2`, `jdbc/as400_a`, and
`jdbc/as400_b`. They are available to `GenericDao` and external JAR code under
`java:comp/env/jdbc/...`. Each resource uses Hikari's standard JNDI factory; switching from H2 to
MySQL only changes its driver, JDBC URL, username, and password settings.

Run with the Jabba-managed JDK 21:

```powershell
. C:\Users\bin\.jabba\jabba.ps1
jabba use temurin@21
mvn clean test
```

## MySQL DAO proof

Run `app-a/src/main/resources/db/mysql-migration-support.sql` once against the
existing SSM MySQL fixture, then start app-a with the `mysql` profile. The REST
endpoint below inserts a policy through the main project's original
`PolicyDaoImpl` and inserts a customer through external-lib-a's
`ExternalJarGenericDao`:

```text
POST http://localhost:18080/rest/mainNouternal/dao
Content-Type: application/json

{
  "policyNo": "SB35-001",
  "holderName": "Spring Boot 3.5 Migration",
  "productName": "CXF 4.1.4",
  "status": "ACTIVE",
  "customerName": "Boot35 Customer 001"
}
```

The main row is stored in `cxfdemo1.policy_info`; the external JAR row is stored
in `cxfdemo2.customers`. The CXF audit interceptors write request and response
rows through `AuditLogDao` and `mapper/AuditLog.xml`.

## REST discovery

`AppACxfConfiguration` scans `com.example.cxfdemo.rest` and
`com.example.cxfdemo.provider`, including classes annotated with JAX-RS `@Path`
or `@Provider`. Spring creates these beans and injects their dependencies;
`cxf.jaxrs.component-scan=true` publishes them with `cxf.jaxrs.server.path=/`.
Adding a resource in these packages requires no new `@Bean` method and no entry
in `setServiceBeans`. If a service uses different packages, adjust its scan roots.
The single servlet registration preserves `/rest/*` and `/Webservice/*`; it is
application infrastructure, not a registration per resource. This discovery is
for JAX-RS REST resources/providers; SOAP endpoint publication is separate.

## Main database contract for every BaseDao consumer

`sharedservices` is a shared JAR, not a separately deployed database service.
Any Spring-managed DAO in an application depending on it can extend
`com.example.cxfdemo.dao.BaseDao`. The inherited fields explicitly select:

| BaseDao field | Required bean | Database path |
| --- | --- | --- |
| `sqlSessionTemplate` | `sqlSessionTemplate1` | `sqlSessionFactory1` → `dataSource1` |
| `jdbcTemplate` | `jdbcTemplate1` | `dataSource1` |
| `dataSource` | `dataSource1` | `java:comp/env/jdbc/cxfdemo1` |

These qualifiers apply to subclasses in any consumer module. They prevent a
second datasource/template (even one marked `@Primary`) from silently replacing
the main dependency. Missing required beans fail startup rather than falling
back. Existing setters remain for legacy compatibility; callers must not use
them to replace the main dependencies with another database.

Each deployed app has its own Spring context, JNDI context, and connection pool.
To use the same main database, configure each app's `jdbc/cxfdemo1` resource with
the same main database host/database, and keep early system-properties JDBC
settings aligned with it. A JNDI name alone does not guarantee the same physical
database. Do not override the reserved `*1` beans with secondary database beans.
`app-b` needs deployment datasource/JNDI settings if later jobs use the main
database. `app-c` and `app-d` remain skeletons. This is not yet a completed
multi-service database deployment.

Verification includes HTTP discovery of a test-only resource without explicit
registration, actual app-a JNDI datasource identity, and two independent Spring
contexts reading the main database through all three BaseDao access paths while
secondary beans are marked `@Primary`. A missing-main-bean test checks fail-fast
behavior. These are H2 checks, not a new multi-process MySQL deployment test.

## app-b scheduler service

`app-b` is a non-web Spring Boot process dedicated to the legacy scheduled jobs.
It enables Spring `@Scheduled` processing for the four original cron methods and
uses Boot's Quartz auto-configuration for the original `QuartzDemoJob`. The
Quartz trigger retains the one-second startup delay and five-minute repeat
interval, using the default in-memory job store. Because app-b has no servlet
server or CXF endpoint, scheduled invocations do not pass through the shared CXF
audit interceptors.

The legacy manual MVC/CXF endpoints that invoked backup cleanup are not part of
the scheduler service migration. The underlying `cleanOldBackupFiles` method is
still available in `ScheduledTasks`. `TransferTaskDao` belongs to the separate
asynchronous transfer workflow and is not part of these scheduled jobs.
````


## File: MIGRATION-HANDOFF.md

````markdown
# SSM → Spring Boot 3.5 遷移交接紀錄

> 新對話請先完整閱讀本文件，再檢查目前 `git status` 與最近的 commit。除非使用者明確要求，請只操作 `ssm-cxf-webservice-demo-springboot35`，不要修改其他專案。

## 1. 使用者要求與不可違反的原則

- 這是在模擬舊 SSM 系統遷移到 Spring Boot。
- 原始專案：`E:\antigravity_workspace\ssm-cxf-webservice-demo`
- 目前要繼續的專案：`E:\antigravity_workspace\ssm-cxf-webservice-demo-springboot35`
- 另有 Boot 4 參考專案：`E:\antigravity_workspace\ssm-cxf-webservice-demo-springboot`
- 目標版本：Spring Boot 3.5.16、Apache CXF 4.1.4、Java 21。
- 多模組固定為父 POM，下有：`common-core`、`sharedservices`、`app-a`、`app-b`、`app-c`、`app-d`。
- 舊程式碼必須盡可能原樣搬移。不要為了風格、簡潔或「最佳實務」重寫。
- 若舊程式真的無法直接使用，必須先說明原因、必要修改與替代方案。
- 功能採一項一項驗收；不要自行一次搬完整個系統。
- 技術問題先用原始碼、實際建置／執行結果或官方資料查證，不確定時不要猜。
- 定時任務尚未開始做。CXF interceptor 只應作用於 Controller／REST／SOAP 呼叫；未來定時任務不需要走 interceptor。

## 2. Git 與 GitHub 狀態

- GitHub：<https://github.com/bin30678/ssm-cxf-webservice-demo-springboot35>
- 分支：`master`
- 已推送的最新 commit：`2bd05fb8de4bb2c2073ae22f50c0fd38ee513cfd`
- 最新三個 commit：
  - `4ac2e7b Create Spring Boot 3.5 CXF 4.1 migration baseline`
  - `a7558bf Add MySQL DAO integration REST proof`
  - `2bd05fb Move shared DAO and services to sharedservices`
- `2bd05fb` 推送後，本地 `HEAD` 與 `origin/master` 相同，工作目錄原本乾淨。
- 本交接文件建立後會成為尚未提交的新檔；除非使用者要求，不要自行 commit／push。
- 不要把任何 GitHub token、MySQL 密碼或其他憑證寫進 Git。先前推送使用電腦既有的 HTTPS credential，沒有把聊天中的 token 寫入專案或 remote URL。

## 3. Java 與建置環境

- 使用者已安裝 Jabba。
- Java 21 安裝位置：`C:\Users\bin\.jabba\jdk\temurin@21`
- 目前 `C:\Users\bin\.jabba\jdk\default` 實際仍是 Java 8。
- 如果直接執行 `mvn test`，Maven 可能使用 Java 8，並對 Java pattern matching 語法報出假的 `')' expected` 等錯誤。
- PowerShell 執行測試前應明確切換：

```powershell
$env:JAVA_HOME='C:\Users\bin\.jabba\jdk\temurin@21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
mvn test
```

- 最近一次完整 reactor 測試使用 Temurin 21.0.6：七個模組全部 `SUCCESS`。
- `app-a` 共 3 個整合測試，結果為 3 tests、0 failures、0 errors。

## 4. 已完成的專案骨架

父 POM 已設定：

- Spring Boot parent 3.5.16
- Java release 21
- Apache CXF BOM 4.1.4
- 六個子模組：`common-core`、`sharedservices`、`app-a`～`app-d`

目前 `app-a` 已有實際功能；`app-b`、`app-c`、`app-d` 目前主要是可編譯的 Spring Boot 應用骨架，尚未搬入各自業務功能。

## 5. system_properties 啟動早期載入

### 原始需求

舊 SSM 的 `DatabasePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer` 會從 `system_properties` table 載入資料。Spring Boot 啟動時，`application.properties` 裡的 `${...}` 與程式中的 `@Value` 都必須取得資料庫值。

### 已採用作法

- 實作位置：
  `sharedservices/src/main/java/com/example/sharedservices/config/DatabaseSystemPropertiesEnvironmentPostProcessor.java`
- 使用 Spring Boot `EnvironmentPostProcessor`，在 ApplicationContext 建立及 `@Value` 解析前，把資料庫內容加入 `Environment`。
- 預設 SQL：
  `SELECT prop_key, prop_value FROM system_properties`
- 設定前綴：`sharedservices.system-properties.*`
- Boot 3.5 的註冊方式在：
  `sharedservices/src/main/resources/META-INF/spring.factories`
- 必須使用這個 Boot 3 可辨識的 `spring.factories` key。先前沿用 Boot 4 專案的註冊方式時，post processor 不會執行，`${...}` 會無法解析；目前已修好。

### 曾遇到的問題與原因

最初讓 EnvironmentPostProcessor 直接查 `jdbc/cxfdemo1` JNDI，啟動時發生：

```text
Failed to load system_properties from JNDI DataSource 'jdbc/cxfdemo1'
```

原因是 EnvironmentPostProcessor 執行得非常早，內嵌 Tomcat 與它的 JNDI context 尚未建立。現在直接使用唯一的主庫資源定義 `sharedservices.jndi.datasources.cxfdemo1.*` 做啟動早期 JDBC 查詢；同一份定義稍後註冊成 `jdbc/cxfdemo1`，不再另設 `spring.datasource.*`。程式仍支援用 `sharedservices.system-properties.jndi-name` 指定啟動前已存在的 JNDI。

### 已驗證

- `system.app.message=DB_STARTUP_PROPERTY_LOADED` 能直接從 Environment 取得。
- `app-a.application-message=${system.app.message}` 能解析。
- `@Value` 能取得相同資料庫值。
- `GenericDao.getSystemProperty(...)` 能查到相同 table 資料。
- 測試：`app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java`

## 6. 內嵌 Tomcat JNDI 與資料庫連線

### 已完成

- 自動設定：
  `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedTomcatJndiAutoConfiguration.java`
- 設定物件：
  `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedJndiProperties.java`
- 自動設定註冊：
  `sharedservices/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 內嵌 Tomcat 啟用 Naming，註冊可由主程式與外部 JAR 共同查找的 `java:comp/env/...` DataSource。

### JNDI 名稱分配

主專案 `sharedservices` 內的原始 `GenericDao` 保留四個 lookup：

- `jdbc/cxfdemo1`
- `jdbc/cxfdemo2`
- `jdbc/as400_a`
- `jdbc/as400_b`

外部 `external-lib-a` 自己的 `GenericDao` 使用：

- `jdbc/cxfdemo2`
- `jdbc/as400_c`

合計註冊五個名稱。H2 測試模式與 MySQL profile 都已配置。

### 為什麼 sharedservices 直接依賴 spring-boot-starter-tomcat

雖然 `app-a` 的 `spring-boot-starter-web` 會帶入內嵌 Tomcat，但 `sharedservices` 自己的 Java 程式直接 import 並繼承 Tomcat embedded 類別，因此該模組編譯時需要直接依賴 `spring-boot-starter-tomcat`。這不是另外啟動第二台 Tomcat。

## 7. GenericDao 與 external-lib-a

- 主專案 `GenericDao` 已搬到：
  `sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java`
- 使用者要求模擬遷移，因此先前做過的優化已撤回，內容維持原 SSM 寫法。
- 外部 JAR dependency：`com.external:external-lib-a:1.0.0`
- 本機實際載入位置：
  `C:\Users\bin\.m2\repository\com\external\external-lib-a\1.0.0\external-lib-a-1.0.0.jar`
- 外部 library 的工作區來源／建置資料夾另有：
  - `E:\antigravity_workspace\external-lib-a`
  - `E:\antigravity_workspace\external-lib-a-build-1.0.0`
  - `E:\antigravity_workspace\external-lib-a-jakarta-v3`
- `AppAExternalLibAJndiProbe` 會驗證執行時載入來源確實是 `.m2` 裡的外部 JAR，而不是把類別複製進主專案。

## 8. 主 DAO、MyBatis XML 與真實 MySQL 驗證

### sharedservices 共用資料庫元件

`MainDatabaseAutoConfiguration` 建立並命名：

- `dataSource1` → JNDI `jdbc/cxfdemo1`
- `sqlSessionFactory1`
- `sqlSessionTemplate1`
- `jdbcTemplate1`
- `transactionManager1`

MyBatis mapper 使用 `classpath*:mapper/*.xml` 載入。

### AuditLogDao

- `AuditLogDao` 位於 sharedservices。
- SQL 已放回 MyBatis XML：
  `sharedservices/src/main/resources/mapper/AuditLog.xml`
- `AuditLogDao` 使用原來的 `SqlSessionTemplate` 呼叫 mapper，不把 SQL 改寫進 Java。

### PolicyDaoImpl 與 BaseDao

- `BaseDao` 已從 `app-a` 搬到：
  `sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java`
- 初次搬移內容和原 SSM 檔案相同；後續為多資料來源明確綁定主庫，僅新增三個 `@Qualifier`，詳見第 14 節。
- `PolicyDaoImpl` 仍在 `app-a`，因為它是 app-a 業務 DAO。
- 先前 `AppACxfConfiguration` 有一段手動 `new PolicyDaoImpl()` 再呼叫三個 setter 的 `@Bean`。它只是為了解決 `com.example.appa` 預設掃描不到 sibling package `com.example.cxfdemo`。
- 目前已刪除手動 setter wiring，改由 `@Import(PolicyDaoImpl.class)` 註冊，並讓 `BaseDao` 原有的 `@Autowired` 欄位接受 `sqlSessionTemplate1`、`jdbcTemplate1`、`dataSource1`。

### REST DAO 驗證入口

- Resource：`app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java`
- URL：`POST /rest/mainNouternal/dao`
- 主專案 `PolicyDaoImpl` 寫入 `cxfdemo1.policy_info`。
- 外部 JAR `ExternalJarGenericDao` 寫入 `cxfdemo2.customers`。
- H2 整合測試：`MainNouternalResourceIntegrationTest`

### 真實 MySQL 已驗證

- profile：`app-a/src/main/resources/application-mysql.properties`
- 建表／測試資料 SQL：`app-a/src/main/resources/db/mysql-migration-support.sql`
- 已在本機 MySQL 實際確認兩邊都有資料：
  - `cxfdemo1.policy_info`：主專案 DAO 寫入成功。
  - `cxfdemo2.customers`：外部 JAR DAO 寫入成功。
  - audit request／response 紀錄也有寫入。
- 一次已驗證的資料範例：
  - policy no：`SB35-20260923074256`
  - customer id：`38`
  - audit guid：`mysql-20260923074256`
- 不要把 MySQL 密碼寫入交接文件或 commit；使用環境變數 `MYSQL_USERNAME`、`MYSQL_PASSWORD`。

## 9. CXF REST／SOAP 與 interceptor

### 路徑

- CXF servlet mapping：`/rest/*`、`/Webservice/*`
- REST health：`GET /rest/health`
- REST DAO proof：`POST /rest/mainNouternal/dao`
- SOAP 測試 endpoint：`/Webservice/soap/audit`（整合測試建立）

### 共用 interceptor

以下類別已移到 sharedservices：

- `AuditRequestInterceptor`
- `AuditResponseInterceptor`
- `AuditFaultInterceptor`
- `UnifiedFaultInterceptor`
- 相關 `ApiError`、`ServiceFaultException`、`WebUtils`

`CxfAuditInterceptorAutoConfiguration` 把 interceptor 註冊到 CXF Bus：

- REST 與 SOAP 正常請求會有 request／response audit。
- REST 不支援的 content type 會回 415，並經過 fault interceptor。
- SOAP fault 會回 500，並經過 fault interceptor。
- response security/cache headers 與 request ID 已測試。
- 因為註冊在 CXF Bus，未來排程工作直接呼叫 service 時不會觸發這些 interceptor，符合使用者要求。

測試：`app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java`

## 10. GsonProvider 與共用 API／Mail service

- `GsonProvider` 已移到 sharedservices：
  `sharedservices/src/main/java/com/example/cxfdemo/provider/GsonProvider.java`
- `BaseDao`、`ApiService`、`ApiServiceImpl`、`MailService`、`MailServiceImpl` 被認定為共用元件，均已移到 sharedservices。
- 連帶依賴也一起搬移：
  - `ConfigSingleton`
  - `ExternalApiLogDao`
  - `ExternalApiLog.xml`
  - `Mail` model
- `ApiServiceImpl` 需要 Apache HttpClient 4.5.2，因此 sharedservices 保留舊版 dependency；排除 `commons-logging`，避免與 Spring 的 `spring-jcl` 重複。
- `MailServiceImpl` 需要 `spring-boot-starter-mail`。
- 搬移檔案已和原 SSM 逐檔比較。只有兩個 Spring Boot 3 必要的 namespace 改動：
  - `javax.annotation.Resource` → `jakarta.annotation.Resource`
  - `javax.mail.internet.MimeMessage` → `jakarta.mail.internet.MimeMessage`
- 其他程式邏輯、註解及格式保持原樣。
- `DatabaseSystemPropertiesStartupTest` 已確認 Spring Context 能取得 `ApiService`、`MailService`、`ExternalApiLogDao`。

## 11. 最近一次完整驗證結果

以 Java 21 執行 `mvn test`：

- parent：SUCCESS
- common-core：SUCCESS
- sharedservices：SUCCESS
- app-a：SUCCESS
- app-b：SUCCESS
- app-c：SUCCESS
- app-d：SUCCESS
- app-a tests：3 passed

同一次測試確認：

- H2 `system_properties` 啟動早期載入。
- `${...}` 與 `@Value` 解析。
- 主 `GenericDao` 四個 JNDI lookup。
- 外部 JAR `GenericDao` 兩個 JNDI lookup。
- 外部類別來源確實為 external-lib-a JAR。
- REST 與 SOAP interceptor 正常／fault 流程。
- REST 主 DAO 與外部 JAR DAO 寫入、查詢。
- 共用 ApiService／MailService／ExternalApiLogDao bean 註冊。

## 12. 目前尚未完成／等待使用者下一步

- 尚未搬入定時任務；使用者已明確說之後才做。
- `app-b`～`app-d` 尚未搬業務服務。
- ApiService 與 MailService 目前已成功註冊並能啟動，但尚未依照真實外部 API／SMTP 環境做端對端呼叫驗證。
- 目前不要擴大重構 GenericDao、BaseDao、service 或 interceptor。
- 每一個新功能完成後，先呈現可驗證結果，由使用者逐項驗收。

## 13. 新對話建議的第一步

```powershell
cd E:\antigravity_workspace\ssm-cxf-webservice-demo-springboot35
git status --short
git log -3 --oneline
$env:JAVA_HOME='C:\Users\bin\.jabba\jdk\temurin@21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

然後依照使用者的新指示，只處理下一個指定功能。

## 14. Resource 自動掃描與所有 BaseDao 使用端的主庫契約

使用者指出 Resource 不應逐一手寫 `@Bean`，並要求 BaseDao 在所有服務中都固定使用原主專案 DB。

### Resource 註冊

- `AppACxfConfiguration` 改為掃描 `com.example.cxfdemo.rest` 與 `com.example.cxfdemo.provider`。
- 保留 Spring 預設 component filters，另納入 JAX-RS `@Path`、`@Provider`，所以既有 Resource 不必再加 `@Component` 或改寫建構子。
- `application.properties` 啟用 `cxf.jaxrs.component-scan=true` 與 `cxf.jaxrs.server.path=/`，由 CXF 發布 Spring 管理的 Resource／Provider。
- 刪除 Resource／GsonProvider 的逐項 `@Bean` 與手動 `appARestServer`／`setServiceBeans` 清單。
- `PolicyDaoImpl` 與外部 `ExternalJarGenericDao` 由 `@Import` 註冊。
- 保留唯一的 ServletRegistrationBean，以維持 `/rest/*`、`/Webservice/*` 舊網址。它不是每個 Resource 各建一個。
- 此掃描是 REST JAX-RS 註冊；不要宣稱 SOAP endpoint 也已自動掃描發布。

### BaseDao 主庫綁定

- `BaseDao.sqlSessionTemplate` 加上 `@Qualifier("sqlSessionTemplate1")`。
- `BaseDao.jdbcTemplate` 加上 `@Qualifier("jdbcTemplate1")`。
- `BaseDao.dataSource` 加上 `@Qualifier("dataSource1")`。
- 其餘 DAO 方法、setter、業務邏輯不變。保留的 setter 不得被使用端另行改塞副庫。
- 這是針對多資料來源新增的必要限定：原本裸 `@Autowired` 只靠型別／Primary 選取，無法在新增其他 Template／DataSource 時持續保證主庫。
- `MainDatabaseAutoConfiguration` 原有鏈路不變：兩種 Template → `dataSource1` → `java:comp/env/jdbc/cxfdemo1`；主庫交易管理器仍為 `transactionManager1`。
- `sharedservices` 是共用 JAR，不是獨立 DB 服務。任一 app 的 Spring-managed DAO 繼承 BaseDao，都會套用相同限定，不限於 sharedservices 內的 DAO。
- 各 app 有自己的 Spring Context／JNDI／連線池；相同 JNDI 名稱不等於同一個實體 DB。所有服務必須把主庫 JDBC URL 指向同一主庫位置，啟動早期 system_properties 連線設定也須一致。
- 既有 `@ConditionalOnMissingBean` 允許使用端覆寫保留的 `*1` beans；不能把副庫註冊成這些主庫名稱。
- app-b～app-d 仍是骨架，本次未替它們建立正式部署 DB 設定，不應宣稱已完成四個服務的 MySQL 部署驗證。

### 測試內容

- `ScannedProbeResource` 只放在 test source，使用 `@Path` 與建構子注入 DAO，不新增任何手動 resource registration；整合測試透過 HTTP 驗證它被自動發布且能讀主庫。
- `MainNouternalResourceIntegrationTest` 新增三個 BaseDao 存取元件指向相同 `dataSource1` 與實際 H2 JNDI URL 的斷言，保留主 DAO／外部 JAR DAO／audit 寫入驗證。
- `BaseDaoMainDatabaseTest` 建立兩個獨立 Spring contexts，第二組 DataSource／JdbcTemplate／SqlSessionTemplate 刻意標記 `@Primary`；驗證 BaseDao 三條存取路徑仍讀到 MAIN 標記，並測試缺少主庫 bean 時啟動失敗而不回退副庫。
- `CxfAuditInterceptorIntegrationTest.AuditProbeConfiguration` 改用 `@TestConfiguration`，避免測試專用 mock DAO／SOAP endpoint 被其他啟動測試掃入；仍由該測試明確傳入啟動來源。
- 驗證結果以本次執行的 `migration-verification.log` 及 Surefire reports 為準；此 log 被 Git 忽略。
- 2026-09-23 使用 Java 21 執行 `mvn -e test`：七個 reactor 模組全部 SUCCESS；app-a 共 5 tests、0 failures、0 errors。先前沙箱執行出現 `Cannot close compiler resources`，改由沙箱外重跑通過。此次未重跑真實 MySQL。

官方依據：
- CXF Spring Boot JAX-RS discovery：<https://cwiki.apache.org/confluence/display/CXF20DOC/SpringBoot>
- Spring 明確限定注入候選：<https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html>

## 15. Servlet Listener 搬入 sharedservices

- 原 SSM `web.xml` 的 `TiffImageReaderCheckListener`、`FontCheckListener` 已搬到 sharedservices。
- 僅做 Spring Boot 3 必要 namespace 調整：`javax.servlet.*` → `jakarta.servlet.*`；檢查邏輯、ServletContext attribute 與 getter 保持原樣。
- 新增 `SystemEnvironmentListenerAutoConfiguration`，只在 Servlet Web Application 啟用，使用 `ServletListenerRegistrationBean` 依原 web.xml 順序註冊 TIFF、Font listener。
- 預設啟用；可用 `sharedservices.environment-check.enabled=false` 關閉。
- app-a 以 `server.servlet.context-parameters.targetFont=標楷體` 保留舊 web.xml 的 context-param。
- 舊 `ContextLoaderListener` 不搬；Spring Boot 自己管理 ApplicationContext 生命週期。
- `DatabaseSystemPropertiesStartupTest` 驗證兩個 listener 寫入的 TIFF、字型 attribute，以及 `targetFont=標楷體`。
- Spring Boot 官方依據：<https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.embedded-container.servlets-filters-listeners>

## 16. 定時任務搬到 app-b

- Listener 整合已先獨立提交：`ff73042 Move servlet environment listeners to sharedservices`。
- 原 SSM 的 `ScheduledTasks` 原樣搬到 app-b，package 仍為 `com.example.cxfdemo.scheduler`。
- 四個 cron 保持不變：每小時 `0 0 * * * *`、午夜 `0 0 0 * * *`、凌晨一點 `0 0 1 * * *`、正午 `0 0 12 * * *`。
- 凌晨一點仍呼叫 `cleanOldBackupFiles("C:/backup_folder", 7)`；檔案清理邏輯未重寫。
- 原 `QuartzDemoJob` 原樣搬到 app-b。以 Boot `spring-boot-starter-quartz` 自動建立 Scheduler，JobDetail 保持 durable；Trigger 啟動延遲 1 秒、每 300000ms 重複，使用預設 RAMJobStore。
- `AppBSchedulingConfiguration` 使用 `@EnableScheduling` 取代舊 XML `<task:annotation-driven/>`，並建立 Boot 會自動收集的 Quartz `JobDetail`、`Trigger` beans。
- app-b 設定為 `spring.main.web-application-type=none`，排程不啟動 Web/CXF，也不經 CXF interceptor。
- 舊 `TaskController` 與 `PolicyResourceImpl.testCleanBackup` 是手動 HTTP 入口，未搬入非 Web 的 app-b；底層 `cleanOldBackupFiles` 保留。
- `TransferTaskDao` 屬於 async transfer 流程，沒有被任何排程類別引用，本次不搬。
- `AppBSchedulingIntegrationTest` 驗證 app-b 註冊 4 個 Spring cron、Quartz durable JobDetail、關聯 Trigger 與 300000ms interval。
- `ScheduledTasksTest` 使用 JUnit temp directory 驗證遞迴刪除超過 7 天的檔案、保留新檔並刪除空子目錄。
- Java 21 執行 `mvn -pl app-b -am test`：app-b 2 tests、0 failures、0 errors，reactor 四個模組 SUCCESS。
- Java 21 完整執行 `mvn test`：七個 reactor 模組全部 SUCCESS；app-a 5 tests、app-b 2 tests，合計 7 tests、0 failures、0 errors。
- 官方依據：Spring `@EnableScheduling` 等價取代 XML annotation-driven：<https://docs.spring.io/spring-framework/reference/integration/scheduling.html>；Boot 自動收集 Quartz JobDetail/Trigger：<https://docs.spring.io/spring-boot/3.5/reference/io/quartz.html>。

## 17. CXF interceptor 共用化與舊 REST 路徑驗證

- `CxfAuditInterceptorAutoConfiguration` 仍只在存在 CXF `Bus` 時啟用；app-b 這類非 Web／無 Bus 服務不會建立或掛載 interceptor。
- 四個 interceptor 改成由 Spring 管理的 beans；三個 audit interceptor 直接依賴 sharedservices 的 `AuditLogDao` bean，缺少 DAO 時不會靜默略過 audit。使用端可提供同型別 bean 覆寫預設實作；共用註冊器只負責把它們掛到 Bus，關閉 Context 時再移除。
- 各 Web 服務只要依賴 sharedservices 並正常建立 CXF Bus，就會自動套用 audit interceptor；不需要在每個服務或每個 Resource 重複列 interceptor。
- 路徑仍維持舊架構的三段組合：CXFServlet `/rest/*` + JAX-RS server `address="/"` + Resource `@Path`。因此 `/rest/...`、`/rest/savxxx/...`、`/rest/ctbcxxxx/...`、`/rest/xxx/...` 共用同一個根 server 與同一組 Bus interceptor，不應為四種前綴建立四個 server。
- test-only `LegacyPathProbeResource` 與 `CxfAuditInterceptorIntegrationTest` 已透過真實 HTTP 驗證一般 `/rest/health` 以及 `savxxx`、`ctbcxxxx`、`xxx` 三種前綴全部產生 audit request，既有 REST/SOAP normal/fault 驗證仍通過。
- Java 21 在沙箱外完整執行 `mvn test`：七個 reactor 模組全部 SUCCESS；app-a 5 tests、app-b 2 tests，合計 7 tests、0 failures、0 errors。
- 官方依據：CXF Bus interceptor 會套用到該 Bus 的所有 endpoints：<https://cxf.apache.org/docs/bus-configuration.html>、<https://cxf.apache.org/docs/interceptors.html>；JAX-RS `@Path` 負責相對資源路徑：<https://cxf.apache.org/docs/jax-rs-basics.html>。

## 18. app-a JAX-RS OpenAPI 與 Swagger UI

- app-a 加入 CXF 4.1.4 官方 `cxf-rt-rs-service-description-openapi-v3` 模組及其 parent 管理版本一致的 Swagger UI WebJar 5.30.2。
- `AppACxfConfiguration` 只建立一個 `OpenApiFeature` bean；該類別本身帶有 CXF `@Provider(Type.Feature, Scope.Server)`，既有 JAX-RS component-scan server 會自動掛載，不需改每一個 Resource，也不需另外建立 server。
- OpenAPI 標題為 `App A REST API`、版本 `1.0.0`，並開啟 Swagger UI 的 Try it out。
- JSON 規格網址：`/rest/openapi.json`。
- Swagger UI 網址：`/rest/api-docs/`。瀏覽器實測發現 Swagger UI 5 WebJar 的預設 `swagger-initializer.js` 仍指向 Petstore，因此新增一個標記 `@Hidden` 的內部 JAX-RS Resource，只覆寫該固定檔案，使 UI 實際讀取 `/rest/openapi.json`；此 Resource 不會出現在 OpenAPI paths，也不影響既有業務 Resource。
- `CxfAuditInterceptorIntegrationTest` 透過真實 HTTP 驗證規格為 OpenAPI 3、包含 `/health`、不包含內部 initializer 路徑，且 Swagger UI HTML 與 initializer 均能載入並指向正確規格網址。
- Java 21 完整執行 `mvn -o test`：七個 reactor 模組全部 SUCCESS；app-a 5 tests、app-b 2 tests，合計 7 tests、0 failures、0 errors。
- 官方依據：CXF OpenAPI Feature <https://cxf.apache.org/docs/openapifeature.html>；CXF 4.1.4 parent 的 Swagger UI 版本 <https://central.sonatype.com/artifact/org.apache.cxf/cxf-parent/4.1.4>。
````

