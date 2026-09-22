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
startup it uses the normal `spring.datasource.*` connection settings, queries `system_properties`,
and adds all `prop_key` / `prop_value` rows as a high-priority Spring property source. This happens
before beans are created, so both placeholders in `application.properties` and `@Value` can use
the values. An existing application-server JNDI DataSource remains available as an explicit
option through `spring.datasource.jndi-name`.

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
