# Project tree

The tree below is generated from the current project and contains all 74 files included in this snapshot. `.git/`, `.idea/`, build output, logs, the previous generated `ai-knowledge/` directory, and `AI_CONTEXT/` itself are omitted.

```text
ssm-cxf-webservice-demo-springboot35/
├── app-a/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           ├── appa/
│   │   │   │           │   ├── config/
│   │   │   │           │   │   └── AppACxfConfiguration.java
│   │   │   │           │   ├── openapi/
│   │   │   │           │   │   └── SwaggerUiInitializerResource.java
│   │   │   │           │   ├── AppAApplication.java
│   │   │   │           │   ├── AppAExternalLibAJndiProbe.java
│   │   │   │           │   ├── AppAJndiConnectionProbe.java
│   │   │   │           │   └── AppAPropertyProbe.java
│   │   │   │           └── cxfdemo/
│   │   │   │               ├── dao/
│   │   │   │               │   ├── PolicyDao.java
│   │   │   │               │   └── PolicyDaoImpl.java
│   │   │   │               ├── model/
│   │   │   │               │   └── PolicyInfo.java
│   │   │   │               └── rest/
│   │   │   │                   ├── HealthResource.java
│   │   │   │                   └── MainNouternalResource.java
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   ├── app-a-jndi-probe.sql
│   │   │       │   ├── app-a-system-properties.sql
│   │   │       │   └── mysql-migration-support.sql
│   │   │       ├── application-mysql.properties
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   ├── appa/
│   │                   │   ├── AuditProbePort.java
│   │                   │   ├── AuditProbePortImpl.java
│   │                   │   ├── BaseDaoMainDatabaseTest.java
│   │                   │   ├── CxfAuditInterceptorIntegrationTest.java
│   │                   │   ├── DatabaseSystemPropertiesStartupTest.java
│   │                   │   └── MainNouternalResourceIntegrationTest.java
│   │                   └── cxfdemo/
│   │                       └── rest/
│   │                           ├── LegacyPathProbeResource.java
│   │                           └── ScannedProbeResource.java
│   └── pom.xml
├── app-b/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           ├── appb/
│   │   │   │           │   ├── config/
│   │   │   │           │   │   └── AppBSchedulingConfiguration.java
│   │   │   │           │   └── AppBApplication.java
│   │   │   │           └── cxfdemo/
│   │   │   │               └── scheduler/
│   │   │   │                   ├── QuartzDemoJob.java
│   │   │   │                   └── ScheduledTasks.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── appb/
│   │                       ├── AppBSchedulingIntegrationTest.java
│   │                       └── ScheduledTasksTest.java
│   └── pom.xml
├── app-c/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── appc/
│   │                       └── AppCApplication.java
│   └── pom.xml
├── app-d/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── appd/
│   │                       └── AppDApplication.java
│   └── pom.xml
├── common-core/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── commoncore/
│   │                       └── CommonCoreMarker.java
│   └── pom.xml
├── sharedservices/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── example/
│   │       │           ├── cxfdemo/
│   │       │           │   ├── config/
│   │       │           │   │   └── ConfigSingleton.java
│   │       │           │   ├── dao/
│   │       │           │   │   ├── AuditLogDao.java
│   │       │           │   │   ├── BaseDao.java
│   │       │           │   │   └── ExternalApiLogDao.java
│   │       │           │   ├── fault/
│   │       │           │   │   ├── ApiError.java
│   │       │           │   │   └── ServiceFaultException.java
│   │       │           │   ├── interceptor/
│   │       │           │   │   ├── AuditFaultInterceptor.java
│   │       │           │   │   ├── AuditRequestInterceptor.java
│   │       │           │   │   ├── AuditResponseInterceptor.java
│   │       │           │   │   └── UnifiedFaultInterceptor.java
│   │       │           │   ├── listener/
│   │       │           │   │   ├── FontCheckListener.java
│   │       │           │   │   └── TiffImageReaderCheckListener.java
│   │       │           │   ├── model/
│   │       │           │   │   └── Mail.java
│   │       │           │   ├── provider/
│   │       │           │   │   └── GsonProvider.java
│   │       │           │   ├── service/
│   │       │           │   │   ├── ApiService.java
│   │       │           │   │   ├── ApiServiceImpl.java
│   │       │           │   │   ├── MailService.java
│   │       │           │   │   └── MailServiceImpl.java
│   │       │           │   └── utils/
│   │       │           │       ├── GenericDao.java
│   │       │           │       └── WebUtils.java
│   │       │           └── sharedservices/
│   │       │               ├── config/
│   │       │               │   └── DatabaseSystemPropertiesEnvironmentPostProcessor.java
│   │       │               ├── cxf/
│   │       │               │   └── CxfAuditInterceptorAutoConfiguration.java
│   │       │               ├── database/
│   │       │               │   └── MainDatabaseAutoConfiguration.java
│   │       │               ├── jndi/
│   │       │               │   ├── EmbeddedJndiProperties.java
│   │       │               │   └── EmbeddedTomcatJndiAutoConfiguration.java
│   │       │               └── listener/
│   │       │                   └── SystemEnvironmentListenerAutoConfiguration.java
│   │       └── resources/
│   │           ├── mapper/
│   │           │   ├── AuditLog.xml
│   │           │   └── ExternalApiLog.xml
│   │           └── META-INF/
│   │               ├── spring/
│   │               │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   │               └── spring.factories
│   └── pom.xml
├── .gitignore
├── MIGRATION-HANDOFF.md
├── pom.xml
└── README.md
```
