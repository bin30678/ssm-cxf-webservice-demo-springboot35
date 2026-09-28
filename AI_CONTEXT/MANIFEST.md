# Manifest

## Counts and completeness

- Candidate files scanned after excluding `.git/`, `target/`, `build/`, and `AI_CONTEXT/`: **96**
- Files included with complete content: **74**
- Files explicitly excluded from the candidate set: **22**
- Included files missing from AI_CONTEXT: **0**
- Included files duplicated across source documents: **0**
- Unclassified candidate files: **0**
- Unincluded files that may affect program behavior: **0 identified**

Directories excluded before candidate counting: `.git/`, `target/`, `build/`, and the generated `AI_CONTEXT/` output itself.

## Included file mapping

Each SHA-256 value is calculated from the original file bytes at snapshot generation time.

| Original file | AI_CONTEXT document | SHA-256 |
| --- | --- | --- |
| `.gitignore` | `BUILD_AND_RUNTIME_CONFIG.md` | `32dc0adf49cebd75ac9aa6b804dc919488b81813f00cb7bdc077f83a6a1ac6b0` |
| `MIGRATION-HANDOFF.md` | `PROJECT_DOCUMENTATION.md` | `e4197c6d6e626d2510e91fa44f66331bc6dd76da6329cf6f15828b79f239e2d8` |
| `README.md` | `PROJECT_DOCUMENTATION.md` | `d7bef1c87fbd27d44eeb495964a5b3658d945032cb2d92888d6bee365e4cd3dc` |
| `app-a/pom.xml` | `BUILD_AND_RUNTIME_CONFIG.md` | `bae3b32355e4ab9c522a46f4b5efe8909905db6c0cc32bf2a7fc59f1994b2686` |
| `app-a/src/main/java/com/example/appa/AppAApplication.java` | `APP_A_API_AND_DOMAIN.md` | `b2fa1ac95b6dad960e02187e3da55ccd95f53472c7e40dbfc5af38a18e9e48e2` |
| `app-a/src/main/java/com/example/appa/AppAExternalLibAJndiProbe.java` | `APP_A_API_AND_DOMAIN.md` | `098cdc169e9dd2824de45e6b678733c3ffe9a1eae239a1e1365c665a1c7f8d30` |
| `app-a/src/main/java/com/example/appa/AppAJndiConnectionProbe.java` | `APP_A_API_AND_DOMAIN.md` | `b1cf6752cfb433a54fb62fc0fe130603af6f0a72d46fe41f41f3c67b1b65149b` |
| `app-a/src/main/java/com/example/appa/AppAPropertyProbe.java` | `APP_A_API_AND_DOMAIN.md` | `b3348f10fff3bc52aa29cfd7ee78795fd8b296fec91b88dc84a48ad6d39e5776` |
| `app-a/src/main/java/com/example/appa/config/AppACxfConfiguration.java` | `APP_A_API_AND_DOMAIN.md` | `165a74d374d7358232c990da51057b75d3275a766665d31e3a3cadc57c61e890` |
| `app-a/src/main/java/com/example/appa/openapi/SwaggerUiInitializerResource.java` | `APP_A_API_AND_DOMAIN.md` | `77e055242ed9277cbd6dcc5abf6a5e75afc14866234f1e20c35ad4d76549d1f2` |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDao.java` | `APP_A_API_AND_DOMAIN.md` | `46122e50d168e1937ec7f6fd0cb138f1ebcdaf213afc3eda57e0565d462a9f56` |
| `app-a/src/main/java/com/example/cxfdemo/dao/PolicyDaoImpl.java` | `APP_A_API_AND_DOMAIN.md` | `c96daeb66442244a6da299a68333844e858d4a2dc7b0895908404d2fa0b018e7` |
| `app-a/src/main/java/com/example/cxfdemo/model/PolicyInfo.java` | `APP_A_API_AND_DOMAIN.md` | `11d8d74369c3d0692a76693ef32611514f29f09b59324c8038fa79c415e91723` |
| `app-a/src/main/java/com/example/cxfdemo/rest/HealthResource.java` | `APP_A_API_AND_DOMAIN.md` | `af8d6b5661a4217268f60cbc7ef50044895074a22faebedce7d6b1ffedba0f71` |
| `app-a/src/main/java/com/example/cxfdemo/rest/MainNouternalResource.java` | `APP_A_API_AND_DOMAIN.md` | `fedd4cbec9ce7a2bc520229049278433152f0c5c29afe749a8d2a53600f6f3b7` |
| `app-a/src/main/resources/application-mysql.properties` | `BUILD_AND_RUNTIME_CONFIG.md` | `5b596ed032671893b8997d47ce587cc8ad4d5884bf4066051a7dfbb92cda0b5d` |
| `app-a/src/main/resources/application.properties` | `BUILD_AND_RUNTIME_CONFIG.md` | `2c6eeb0d86f70feffd47f28886c4045ecb04d3a55997bf2fe8a60af72519b7b4` |
| `app-a/src/main/resources/db/app-a-jndi-probe.sql` | `SQL_AND_MYBATIS.md` | `e56833f662cd861fa4111712b54a08e2c7686f73075d632bcc9548d8ceb188a3` |
| `app-a/src/main/resources/db/app-a-system-properties.sql` | `SQL_AND_MYBATIS.md` | `461f90247cd490a53ef47bedc4257baa56b210143680bb662d833fcce53dbd77` |
| `app-a/src/main/resources/db/mysql-migration-support.sql` | `SQL_AND_MYBATIS.md` | `76421a1ec18cdf8d039136257ebb6f9bb0c9140c45167557fe40c69186567c00` |
| `app-a/src/test/java/com/example/appa/AuditProbePort.java` | `TESTS.md` | `051503b277f26a464e7e8a27348872fbb3866bc62809898987306c86bf61ef64` |
| `app-a/src/test/java/com/example/appa/AuditProbePortImpl.java` | `TESTS.md` | `71128d2f9c14344eff0386ae3e03a7bc158829875f1163bc315b553001c47bb5` |
| `app-a/src/test/java/com/example/appa/BaseDaoMainDatabaseTest.java` | `TESTS.md` | `a0ee6286d73b3cd4c4fb2904792f1ffa37a7bee81de8b1671e6f3a6a545ce08f` |
| `app-a/src/test/java/com/example/appa/CxfAuditInterceptorIntegrationTest.java` | `TESTS.md` | `f320a7dd4c5c773d0e9311d1af487c9e40c4a2b027f7fcb6307cde55683a7a77` |
| `app-a/src/test/java/com/example/appa/DatabaseSystemPropertiesStartupTest.java` | `TESTS.md` | `3d150e9d6ac4b343a498e0d6fd0cf57f3e38457d98c9cf7d0b3584bbf095872a` |
| `app-a/src/test/java/com/example/appa/MainNouternalResourceIntegrationTest.java` | `TESTS.md` | `234881f9fdbb464960eec965489194b23835147593fbb05cfc7768223af2e3ce` |
| `app-a/src/test/java/com/example/cxfdemo/rest/LegacyPathProbeResource.java` | `TESTS.md` | `285dcf77cddcf3165f767c35b15b9153e84df8770a0d83b0da826e39283774d1` |
| `app-a/src/test/java/com/example/cxfdemo/rest/ScannedProbeResource.java` | `TESTS.md` | `c1eb1fe9dd434963f014913546e3e544b60061fee7e8dc2276b3d9357dc5877b` |
| `app-b/pom.xml` | `BUILD_AND_RUNTIME_CONFIG.md` | `b7db1d8a201f2aa6ef33f442bef06f7213662c81b561d6beb40bb68b0424f1ed` |
| `app-b/src/main/java/com/example/appb/AppBApplication.java` | `APP_B_SCHEDULING.md` | `5f85a6749b0400ddde06039e1157e0243176013b582b491b4e28de5f8fde6b13` |
| `app-b/src/main/java/com/example/appb/config/AppBSchedulingConfiguration.java` | `APP_B_SCHEDULING.md` | `5e4c59a3a82b53291850e8d81911d6fdc5b7d4529d4a0a8bcf41298b9082ab5c` |
| `app-b/src/main/java/com/example/cxfdemo/scheduler/QuartzDemoJob.java` | `APP_B_SCHEDULING.md` | `2328ea0e24df34339db9cd12f1ae7e4ff5d3ca0b74c8d2c580e24aa35440f1d6` |
| `app-b/src/main/java/com/example/cxfdemo/scheduler/ScheduledTasks.java` | `APP_B_SCHEDULING.md` | `7bb8d1cc8ae3e247822d8e755532fccefaeaac747b095f584961b709e9a28778` |
| `app-b/src/main/resources/application.properties` | `BUILD_AND_RUNTIME_CONFIG.md` | `bbb4a6949013b84c923cb50d6a7225a86363968cd61cb88a7356d72d59009677` |
| `app-b/src/test/java/com/example/appb/AppBSchedulingIntegrationTest.java` | `TESTS.md` | `1f452c9a5761f1e377964ac177f7cdc30c23cabcb88bb3f76533b9944ee61b09` |
| `app-b/src/test/java/com/example/appb/ScheduledTasksTest.java` | `TESTS.md` | `89854fb4ea6d926a93de756331523c38ef5a5945f5c5b22bf720e11fda1441c0` |
| `app-c/pom.xml` | `BUILD_AND_RUNTIME_CONFIG.md` | `1aa3eeebf3d985328f29cde8467112d2f39997217ea4a60b52c700d9b0a74950` |
| `app-c/src/main/java/com/example/appc/AppCApplication.java` | `APP_C_D_AND_COMMON.md` | `0283f4ec34183cfb49ad15c66d9cea9459cd8b1d351f7bbaee07a69fee1e7cfe` |
| `app-d/pom.xml` | `BUILD_AND_RUNTIME_CONFIG.md` | `da6fb8c7e81523161e05bc2e3444ae3feb0bbe058fa6e66c8a1301d06041488f` |
| `app-d/src/main/java/com/example/appd/AppDApplication.java` | `APP_C_D_AND_COMMON.md` | `eb89f39c71673a6f55e723e144d0cf90d33fdb3c3eb7d05daa854fe799fa6cdb` |
| `common-core/pom.xml` | `BUILD_AND_RUNTIME_CONFIG.md` | `79d3cb7cfb4725ef7c05ee381c2b60041f4aaaf0afdc1657173048812bb1b3ce` |
| `common-core/src/main/java/com/example/commoncore/CommonCoreMarker.java` | `APP_C_D_AND_COMMON.md` | `b9c010782692aef2f608a615f8b467a5d34dc0a521d8e539b2b758c2b447fead` |
| `pom.xml` | `BUILD_AND_RUNTIME_CONFIG.md` | `21f0a885b53daa71d205ed781c70193602e1d1a4fa9a88d7ccd757efae508681` |
| `sharedservices/pom.xml` | `BUILD_AND_RUNTIME_CONFIG.md` | `acd0aa729948938082f18cfddc6e59d6af905b0ea73dfd1e00a9a50215ab8869` |
| `sharedservices/src/main/java/com/example/cxfdemo/config/ConfigSingleton.java` | `SHARED_DATABASE_AND_SERVICE.md` | `dc84b3f4bb4e1da769edba458ef5762d620ff7d72cdf78780f9f0ce60093ea9b` |
| `sharedservices/src/main/java/com/example/cxfdemo/dao/AuditLogDao.java` | `SHARED_DATABASE_AND_SERVICE.md` | `a756607cb91ddc068c1bdd81ca2303a8b6decb44bb5a5034979ff2a0d92d2b0d` |
| `sharedservices/src/main/java/com/example/cxfdemo/dao/BaseDao.java` | `SHARED_DATABASE_AND_SERVICE.md` | `12aa6470968689ad9808a8fd1a457b14188eab56c83f542977538965da7c44f3` |
| `sharedservices/src/main/java/com/example/cxfdemo/dao/ExternalApiLogDao.java` | `SHARED_DATABASE_AND_SERVICE.md` | `6791e9bd2598ce990086f7d63a899b72e27c32f9b389019d3cc47c5336a95b72` |
| `sharedservices/src/main/java/com/example/cxfdemo/fault/ApiError.java` | `SHARED_CXF_AND_SERVLET.md` | `2b78e51417e876a0ab6e0a820e526809c9708bb55363a98235bc42f031af7873` |
| `sharedservices/src/main/java/com/example/cxfdemo/fault/ServiceFaultException.java` | `SHARED_CXF_AND_SERVLET.md` | `1098bf076c459790b29253e13a99a47ec80c99b97a2306275f0b8f4b99dc04b2` |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditFaultInterceptor.java` | `SHARED_CXF_AND_SERVLET.md` | `1ee08d33f3b1ec974ed7d1627f591c86355b64a415c0196c7c84a565b887def9` |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditRequestInterceptor.java` | `SHARED_CXF_AND_SERVLET.md` | `834e6bcc2af888689c730d499c0cf4e3383775fa1af14d9640006fe4a12ec332` |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/AuditResponseInterceptor.java` | `SHARED_CXF_AND_SERVLET.md` | `a37b5e32ae68524ba7a7f41fa0dce314076e6fdcbb1f314d48589c5335b6ca68` |
| `sharedservices/src/main/java/com/example/cxfdemo/interceptor/UnifiedFaultInterceptor.java` | `SHARED_CXF_AND_SERVLET.md` | `238fe2f2d866b9b9b2866fb55ac15a9572144542ee2aea03aadcfb6360c9eaea` |
| `sharedservices/src/main/java/com/example/cxfdemo/listener/FontCheckListener.java` | `SHARED_CXF_AND_SERVLET.md` | `3b39ba103aef5aa4889f0c0457ba725846d7ae163654ed6137387a669d4bacdb` |
| `sharedservices/src/main/java/com/example/cxfdemo/listener/TiffImageReaderCheckListener.java` | `SHARED_CXF_AND_SERVLET.md` | `f0c195f50f11fb4e251fb8d2d09667e6d34e6c1f5f7e7f2b6f8f0a05a9feb012` |
| `sharedservices/src/main/java/com/example/cxfdemo/model/Mail.java` | `SHARED_DATABASE_AND_SERVICE.md` | `c8d492fd19d3bf03eb4a711fc977e6e56d588e30b49f511f0d1de211a16896fd` |
| `sharedservices/src/main/java/com/example/cxfdemo/provider/GsonProvider.java` | `SHARED_CXF_AND_SERVLET.md` | `aca02df2f2e509f6b95f5e46215b45bce0d7f5d81bb8fd01587cdc0ab4cbbf13` |
| `sharedservices/src/main/java/com/example/cxfdemo/service/ApiService.java` | `SHARED_DATABASE_AND_SERVICE.md` | `709c3f78ba08c24cacb3e110c7578d06dde9854b8aca8be3cb99826484f96c78` |
| `sharedservices/src/main/java/com/example/cxfdemo/service/ApiServiceImpl.java` | `SHARED_DATABASE_AND_SERVICE.md` | `801175484417bb639824b3ca0e209c53617ee2854bceda4b17741a58bb82eed0` |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailService.java` | `SHARED_DATABASE_AND_SERVICE.md` | `e84d7f279637c4494e6ca1b75d2f1af52511b83e528a2233a0024b9cec4178e2` |
| `sharedservices/src/main/java/com/example/cxfdemo/service/MailServiceImpl.java` | `SHARED_DATABASE_AND_SERVICE.md` | `a358b649a33063c73906913dc08e665b62790523d06a3398898ef8a58da4e5c8` |
| `sharedservices/src/main/java/com/example/cxfdemo/utils/GenericDao.java` | `SHARED_DATABASE_AND_SERVICE.md` | `1844ce018b23760060160849025f295e48a59820e1ebdf5dee47d34c4b0329aa` |
| `sharedservices/src/main/java/com/example/cxfdemo/utils/WebUtils.java` | `SHARED_DATABASE_AND_SERVICE.md` | `f53aa9dcbb36e0bd84d44606f2346bf2699e995387ebd9033bc1cc4ae13b0385` |
| `sharedservices/src/main/java/com/example/sharedservices/config/DatabaseSystemPropertiesEnvironmentPostProcessor.java` | `SHARED_DATABASE_AND_SERVICE.md` | `a8044e9968056cc76e2cf2708e648987fdc9dede1d8f9a64de73cc9975f58a27` |
| `sharedservices/src/main/java/com/example/sharedservices/cxf/CxfAuditInterceptorAutoConfiguration.java` | `SHARED_CXF_AND_SERVLET.md` | `1d5da23a9f2240d24b084bb23b5849c4bd3ca383339a79b280269101f2d7b00e` |
| `sharedservices/src/main/java/com/example/sharedservices/database/MainDatabaseAutoConfiguration.java` | `SHARED_DATABASE_AND_SERVICE.md` | `9a7a5c51a6a957a4493fcc01aba33acd9145b08b79d33f7043cca5ec00cced2b` |
| `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedJndiProperties.java` | `SHARED_DATABASE_AND_SERVICE.md` | `953dae455bfbc357203deda9547bf8149b11412e041366491cad5cadd1e40616` |
| `sharedservices/src/main/java/com/example/sharedservices/jndi/EmbeddedTomcatJndiAutoConfiguration.java` | `SHARED_DATABASE_AND_SERVICE.md` | `1cc87febb5ec377eb4dcd1bd677ee7a0fe3c6aaafd8c7c1d41b726fdcc99a1e0` |
| `sharedservices/src/main/java/com/example/sharedservices/listener/SystemEnvironmentListenerAutoConfiguration.java` | `SHARED_CXF_AND_SERVLET.md` | `4746e49487dcdd5e217a7fd9a3c5bbf0b5f5306c3e97c4b0e5e6698e4bee4a89` |
| `sharedservices/src/main/resources/META-INF/spring.factories` | `BUILD_AND_RUNTIME_CONFIG.md` | `d4cfe72442f92484c67828778bffa3badb8d03ab7622348b78b4dec832d37bda` |
| `sharedservices/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | `BUILD_AND_RUNTIME_CONFIG.md` | `cabbac4bf92acfef0c5f81de9384cab808727b20bc08f86d8cbb639c8aa266df` |
| `sharedservices/src/main/resources/mapper/AuditLog.xml` | `SQL_AND_MYBATIS.md` | `1cf4845eb07a2691210461a5099145b2007de31494db17eae20696903c769fa2` |
| `sharedservices/src/main/resources/mapper/ExternalApiLog.xml` | `SQL_AND_MYBATIS.md` | `fb32fc2240e8b42b035ec2b709118184da5105328c0351aa0361bf1742ad22dd` |

## Excluded files

| Excluded file | Reason |
| --- | --- |
| `.idea/.gitignore` | IDE metadata; excluded by request. |
| `.idea/compiler.xml` | IDE metadata; excluded by request. |
| `.idea/encodings.xml` | IDE metadata; excluded by request. |
| `.idea/jarRepositories.xml` | IDE metadata; excluded by request. |
| `.idea/misc.xml` | IDE metadata; excluded by request. |
| `.idea/vcs.xml` | IDE metadata; excluded by request. |
| `.idea/workspace.xml` | IDE metadata; excluded by request. |
| `ai-knowledge/00-project-index.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/01-maven-and-config.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/02-api-resource.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/03-service-01.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/04-dao.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/05-mybatis-mapper.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/06-spring-configuration.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/07-cxf.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/08-scheduler.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/09-model-and-pojo.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/10-common-utility.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/11-test-suite.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `ai-knowledge/VALIDATION.md` | Previously generated AI snapshot; excluded to prevent recursive/stale duplication. |
| `app-b-migration-verification.log` | Execution/verification log; generated output excluded by request. |
| `migration-verification.log` | Execution/verification log; generated output excluded by request. |

## Unclassifiable files

None.

## Potentially important but not included

No unincluded source/configuration file was identified inside the candidate scan boundary. External dependency source, deployment-system configuration outside this repository, and version-control history are outside the snapshot boundary.

## Validation method

1. Enumerated all files recursively under the project root while excluding `.git/`, `target/`, `build/`, and `AI_CONTEXT/`.
2. Classified every remaining file as included or explicitly excluded.
3. Required the included set and classification set to be exactly equal.
4. Required every included path to occur exactly once as a `## File:` section in exactly one source document.
5. Required the complete decoded original text to occur inside its assigned document.
6. Recorded original-byte SHA-256 values for later comparison.
