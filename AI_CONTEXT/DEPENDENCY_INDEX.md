# Dependency index

This index contains only relationships visible in the current source. Runtime discovery, reflection, external JAR internals, and deployment wiring may add relationships that static analysis cannot confirm.

## Confirmed application flows

- `AppAApplication` starts the app-a Spring Boot context.
- `AppACxfConfiguration` registers one CXF servlet for `/rest/*` and `/Webservice/*`, scans JAX-RS resources/providers, imports `PolicyDaoImpl` and external `ExternalJarGenericDao`, and exposes `OpenApiFeature`.
- `GET /rest/health` -> `HealthResource.health()`.
- `POST /rest/mainNouternal/dao` -> `MainNouternalResource` -> `PolicyDaoImpl` (main database) and external `ExternalJarGenericDao` (external JAR/JNDI path).
- `MainDatabaseAutoConfiguration` creates `dataSource1` -> `sqlSessionFactory1` -> `sqlSessionTemplate1`, plus `jdbcTemplate1` and `transactionManager1`, and imports the shared API/mail service implementations and `ExternalApiLogDao`.
- `BaseDao` explicitly injects `sqlSessionTemplate1`, `jdbcTemplate1`, and `dataSource1`; its subclasses therefore target the named main-database infrastructure when Spring manages them.
- `ApiServiceImpl` reads URLs from `ConfigSingleton`, calls external HTTP endpoints with Apache HttpClient, and records attempts through `ExternalApiLogDao`.
- `MailServiceImpl` uses Spring mail infrastructure and the `Mail` model.
- `CxfAuditInterceptorAutoConfiguration` creates and attaches request, response, fault, and unified-fault interceptors to the CXF `Bus`; audit interceptors use `AuditLogDao`.
- `SystemEnvironmentListenerAutoConfiguration` registers `TiffImageReaderCheckListener` and `FontCheckListener` for servlet applications.
- `AppBApplication` imports `AppBSchedulingConfiguration`; that configuration enables Spring scheduling, imports `ScheduledTasks`, and registers the Quartz `QuartzDemoJob` detail and trigger.
- `DatabaseSystemPropertiesEnvironmentPostProcessor` is loaded through `META-INF/spring.factories`; the four shared auto-configurations are loaded through `AutoConfiguration.imports`.

## Database and mapper resources

- `mapper/AuditLog.xml` supplies statements used by `AuditLogDao`.
- `mapper/ExternalApiLog.xml` supplies statements used by `ExternalApiLogDao`.
- `MainDatabaseAutoConfiguration` loads `classpath*:mapper/*.xml` into `sqlSessionFactory1`.
- The application properties define embedded JNDI datasource metadata. The physical production database identity cannot be confirmed from source alone because deployment configuration may override it.

## Static local-class references

| Source | Referenced local source | Evidence |
| --- | --- | --- |
| `app-a/src/main/java/com/example/appa/AppAJndiConnectionProbe.java` | `sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java` | explicit import and type reference |
| `app-a/src/main/java/com/example/appa/config/AppACxfConfiguration.java` | `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | explicit import and type reference |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDao.java` | `app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java` | explicit import and type reference |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDao.java` | same-package type reference |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | `app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java` | explicit import and type reference |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java` | same-package type reference |
| `app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java` | `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | explicit import and type reference |
| `app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java` | `app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java` | explicit import and type reference |
| `app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java` | `sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/AuditProbePortImpl.java` | `app-a/src/test/java/com/example/appa/AuditProbePort.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/AuditProbePortImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/BaseDaoMainDatabaseTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/BaseDaoMainDatabaseTest.java` | `sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java` | `app-a/src/main/java/com/example/appa/AppAApplication.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java` | `app-a/src/test/java/com/example/appa/AuditProbePortImpl.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `app-a/src/main/java/com/example/appa/AppAApplication.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `app-a/src/main/java/com/example/appa/AppAExternalLibAJndiProbe.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `app-a/src/main/java/com/example/appa/AppAJndiConnectionProbe.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `app-a/src/main/java/com/example/appa/AppAPropertyProbe.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/service/ApiService.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java` | `app-a/src/main/java/com/example/appa/AppAApplication.java` | same-package type reference |
| `app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java` | `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java` | `app-a/src/main/java/com/example/cxfdemo/rest/HealthResource.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/provider/GsonProvider.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java` | `sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java` | explicit import and type reference |
| `app-a/src/test/java/com/example/cxfdemo/rest/ScannedProbeResource.java` | `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | explicit import and type reference |
| `app-b/src/main/java/com/example/appb/config/AppBSchedulingConfiguration.java` | `app-b/src/main/java/com/example/cxfdemo/scheduler/QuartzDemoJob.java` | explicit import and type reference |
| `app-b/src/main/java/com/example/appb/config/AppBSchedulingConfiguration.java` | `app-b/src/main/java/com/example/cxfdemo/scheduler/ScheduledTasks.java` | explicit import and type reference |
| `app-b/src/test/java/com/example/appb/AppBSchedulingIntegrationTest.java` | `app-b/src/main/java/com/example/appb/AppBApplication.java` | same-package type reference |
| `app-b/src/test/java/com/example/appb/AppBSchedulingIntegrationTest.java` | `app-b/src/main/java/com/example/cxfdemo/scheduler/ScheduledTasks.java` | explicit import and type reference |
| `app-b/src/test/java/com/example/appb/ScheduledTasksTest.java` | `app-b/src/main/java/com/example/cxfdemo/scheduler/ScheduledTasks.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java` | `sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java` | same-package type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditFaultInterceptor.java` | `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | same-package type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | `sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | `sharedservices/src/main/java/com/example/cxfdemo/utils/WebUtils.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditResponseInterceptor.java` | `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | same-package type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/UnifiedFaultInterceptor.java` | `sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/UnifiedFaultInterceptor.java` | `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | same-package type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/service/ApiService.java` | same-package type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java` | `sharedservices/src/main/java/com/example/cxfdemo/model/Mail.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/model/Mail.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java` | `sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java` | same-package type reference |
| `sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditFaultInterceptor.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditResponseInterceptor.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/interceptor/UnifiedFaultInterceptor.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedTomcatJndiAutoConfiguration.java` | `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedJndiProperties.java` | same-package type reference |
| `sharedservices/src/main/java/com/example/sharedservices/listener/SystemEnvironmentListenerAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/listener/FontCheckListener.java` | explicit import and type reference |
| `sharedservices/src/main/java/com/example/sharedservices/listener/SystemEnvironmentListenerAutoConfiguration.java` | `sharedservices/src/main/java/com/example/cxfdemo/listener/TiffImageReaderCheckListener.java` | explicit import and type reference |

## Limits

- `com.external:external-lib-a:1.0.0` is a binary dependency. Its complete implementation is outside this repository and is not copied into this snapshot.
- CXF component discovery, Spring conditional auto-configuration, JNDI bindings, and dependency injection have runtime behavior that static source inspection cannot fully resolve.
- app-c and app-d contain only Boot entry points and module build files; deployed responsibilities cannot be determined from current source.
