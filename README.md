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
