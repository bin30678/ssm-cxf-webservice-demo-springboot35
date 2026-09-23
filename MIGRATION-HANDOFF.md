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
