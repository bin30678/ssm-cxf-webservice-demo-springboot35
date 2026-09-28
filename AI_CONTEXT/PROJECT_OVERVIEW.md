# Project overview

## Snapshot scope

This `AI_CONTEXT` directory is a textual snapshot of the current working tree at generation time. It packages every human-readable source/configuration file selected by the rules in `MANIFEST.md`, including current uncommitted source changes. It does not modify the original files.

## Technologies identified from current files

- Maven multi-module build with modules `common-core`, `sharedservices`, `app-a`, `app-b`, `app-c`, and `app-d`.
- Java 21 and Spring Boot 3.5.16.
- Apache CXF 4.1.4 with JAX-RS, JAX-WS dependencies, interceptors, Gson provider, OpenAPI 3, and Swagger UI.
- MyBatis/MyBatis-Spring mapper XML, Spring JDBC, JNDI datasources, embedded Tomcat, HikariCP, H2 runtime support, and MySQL Connector/J.
- Spring scheduling and Quartz in app-b.
- Spring Mail and Apache HttpClient in sharedservices.
- JUnit 5, AssertJ, Mockito, and Spring Boot integration tests.

## Module structure

- `common-core`: marker-only common module in the current source.
- `sharedservices`: shared service/DAO code, database and JNDI auto-configuration, CXF interceptors, provider, fault handling, servlet listeners, utilities, mapper XML, and Boot loading metadata.
- `app-a`: runnable servlet/CXF application with REST resources, OpenAPI/Swagger UI, main-project DAO/domain code, JNDI/startup probes, external-JAR integration, database scripts, and integration tests.
- `app-b`: non-web scheduling application using Spring `@Scheduled` and Quartz, with tests.
- `app-c`, `app-d`: Spring Boot skeleton applications; no business source is present.

## AI_CONTEXT documents

| Document | Purpose | Original files |
| --- | --- | ---: |
| `PROJECT_TREE.md` | Filtered project tree for all included files | index only |
| `BUILD_AND_RUNTIME_CONFIG.md` | Maven POMs, application properties, Git ignore rules, and Boot loading metadata | 13 |
| `APP_A_API_AND_DOMAIN.md` | Complete app-a main Java source | 11 |
| `APP_B_SCHEDULING.md` | Complete app-b main Java source | 4 |
| `APP_C_D_AND_COMMON.md` | common-core marker and app-c/app-d entry points | 3 |
| `SHARED_DATABASE_AND_SERVICE.md` | Shared database, JNDI, DAO, service, model, and utility source | 15 |
| `SHARED_CXF_AND_SERVLET.md` | CXF interceptors/configuration, fault classes, provider, and servlet listeners | 11 |
| `SQL_AND_MYBATIS.md` | SQL scripts and MyBatis mapper XML | 5 |
| `TESTS.md` | Complete app-a and app-b test source | 10 |
| `PROJECT_DOCUMENTATION.md` | Existing README and migration handoff | 2 |
| `DEPENDENCY_INDEX.md` | Confirmed static relationships and analysis limits | analysis only |
| `MANIFEST.md` | One-to-one file mapping, exclusions, hashes, and validation | index only |

## Excluded content

- `.git/`: version-control internals, excluded before counting.
- `target/` and `build/`: generated build output, excluded before counting. No target directory was present after the most recent clean when this snapshot was generated.
- `.idea/`: IDE metadata.
- `*.log`: generated execution/verification logs.
- `ai-knowledge/`: an older generated AI snapshot, excluded to prevent recursive and stale duplication.
- Binary files, JARs, class files, images, and caches: excluded; none outside the excluded directories were needed for this textual source snapshot.
- `AI_CONTEXT/`: output directory, excluded from its own scan.

## Information that cannot be confirmed from current source

- The real deployment hostnames, credentials, and physical production database instances; current properties include demonstration values and may be overridden.
- The complete implementation and runtime behavior of binary dependency `com.external:external-lib-a:1.0.0`.
- Production topology, service ownership, traffic volume, and deployment orchestration.
- Business responsibilities for app-c and app-d beyond being Spring Boot entry-point skeletons.
- Runtime-only relationships introduced through reflection, container/JNDI configuration, or conditional beans that are not active in a particular deployment.
